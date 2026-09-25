#!/usr/bin/env python3
"""Fix NeoForm decompiler artifacts in the transformed source.
Runs on every build after neoFormTransformSource regenerates the sources.
Uses exact string replacements (and a few regexes) for reliability.
"""
import os, re, sys

base = sys.argv[1] if len(sys.argv) > 1 else "."
fixed_files = 0

def fix_file(path, filename):
    with open(path, 'r') as fh:
        content = fh.read()
    original = content

    # --- 1. EntityDataSerializer: lambda on non-functional interface ---
    if 'EntityDataSerializer' in filename:
        content = content.replace(
            'return () -> codec;',
            'return new EntityDataSerializer<T>() {\n'
            '        @Override\n'
            '        public StreamCodec<? super RegistryFriendlyByteBuf, T> codec() { return codec; }\n'
            '        @Override\n'
            '        public T copy(T value) { return value; }\n'
            '    };'
        )

    # --- 2. BlockBehaviour: getMapColor / getPistonPushReaction ---
    # In 26.2 these are state fields; Block has no such methods.
    if 'BlockBehaviour' in filename:
        content = content.replace(
            'public MapColor getMapColor(BlockGetter level, BlockPos pos) {\n            return getBlock().getMapColor(this.asState(), level, pos, this.mapColor);\n        }',
            'public MapColor getMapColor(BlockState state, LevelReader level, BlockPos pos, MapColor color) {\n            return this.mapColor;\n        }'
        )
        content = content.replace(
            'public PushReaction getPistonPushReaction() {\n            PushReaction reaction = getBlock().getPistonPushReaction(asState());\n            if (reaction != null) return reaction;\n            return this.pushReaction;\n        }',
            'public PushReaction getPistonPushReaction() {\n            return this.pushReaction;\n        }'
        )

    # --- 3. Block.java: setValue type mismatch in setValueHelper ---
    if 'Block.java' in filename and 'setValueHelper' in content:
        content = content.replace(
            'private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S state, Property<T> property, Object value) {\n        return state.setValue(property, (Comparable)value);\n    }',
            '@SuppressWarnings("unchecked")\n    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S state, Property<T> property, Object value) {\n        return (S) state.setValue(property, (T) value);\n    }'
        )

    # --- 4. Registry.java: type inference ---
    if 'Registry.java' in filename:
        content = content.replace(
            'return DataFixUtils.orElse(this.get(id), List.<T>of());',
            'return DataFixUtils.orElse(this.get(id), java.util.List.of());'
        )

    # --- 5. ServerEntity.java: cannot find symbol ---
    if 'ServerEntity' in filename:
        content = content.replace(
            'this.entity.sendPairingData(player, payload -> broadcast.accept(payload.toVanillaClientbound()));',
            '// this.entity.sendPairingData(player, payload -> broadcast.accept(payload.toVanillaClientbound()));'
        )

    # --- 6. AttributeModifier: unchecked cast on OverrideModifier.INSTANCE ---
    if 'AttributeModifier' in filename:
        content = content.replace(
            'static <Value> AttributeModifier<Value, Value> override() {\n        return AttributeModifier.OverrideModifier.INSTANCE;\n    }',
            '@SuppressWarnings("unchecked")\n    static <Value> AttributeModifier<Value, Value> override() {\n        return (AttributeModifier<Value, Value>) AttributeModifier.OverrideModifier.INSTANCE;\n    }'
        )

    # --- 7. CustomPayload packets: codec() takes only (fallback, types) ---
    # Drop the trailing ConnectionProtocol/PacketFlow args.
    if filename in ('ClientboundCustomPayloadPacket.java', 'ServerboundCustomPayloadPacket.java'):
        content = re.sub(
            r',\s*net\.minecraft\.network\.ConnectionProtocol\.\w+,\s*net\.minecraft\.network\.protocol\.PacketFlow\.\w+',
            '', content
        )

    # --- 8. CustomPacketPayload: findCodec returns wildcard codec ---
    if 'CustomPacketPayload' in filename and 'writeCap' in content:
        content = content.replace(
            'StreamCodec<B, T> codec = this.findCodec(type.id);',
            '@SuppressWarnings("unchecked")\n                StreamCodec<B, T> codec = (StreamCodec<B, T>) this.findCodec(type.id);'
        )

    # --- 9. IntegerModifier: not a functional interface (3 abstract methods) ---
    if 'IntegerModifier' in filename and 'ADD' in content:
        for name, orig, expr in [
            ('ADD', 'Integer::sum', 'a + b'),
            ('SUBTRACT', '(a, b) -> a - b', 'a - b'),
            ('MULTIPLY', '(a, b) -> a * b', 'a * b'),
            ('MINIMUM', 'Math::min', 'Math.min(a, b)'),
            ('MAXIMUM', 'Math::max', 'Math.max(a, b)'),
        ]:
            content = content.replace(
                'IntegerModifier<Integer> %s = %s;' % (name, orig),
                'IntegerModifier<Integer> %s = new IntegerModifier.Simple() { @Override public Integer apply(Integer a, Integer b) { return %s; } };' % (name, expr)
            )

    # --- 10. MappedRegistry: @Override on non-overriding methods ---
    if 'MappedRegistry' in filename:
        content = content.replace(
            '@Override\n            public HolderLookup.RegistryLookup<T> lookup() {',
            'public HolderLookup.RegistryLookup<T> lookup() {'
        )
        content = content.replace(
            '@Override\n            public Map<TagKey<T>, List<Holder<T>>> contents() {',
            'public Map<TagKey<T>, List<Holder<T>>> contents() {'
        )
        content = content.replace(
            '@Override\n    public int getId(ResourceKey<T> key) {',
            'public int getId(ResourceKey<T> key) {'
        )
        content = content.replace(
            '@Override\n    public boolean containsValue(T value) {',
            'public boolean containsValue(T value) {'
        )

    # --- 11. DefaultedMappedRegistry: @Override on getKeyOrNull ---
    if 'DefaultedMappedRegistry' in filename:
        content = content.replace(
            '@Nullable\n    @Override\n    public Identifier getKeyOrNull(T element) {',
            '@Nullable\n    public Identifier getKeyOrNull(T element) {'
        )

    # --- 12. EntityFlagsPredicate: match binding redefinition ---
    if 'EntityFlagsPredicate' in filename:
        content = content.replace(
            'return this.isFallFlying.isPresent() && entity instanceof LivingEntity living && living.isFallFlying() != this.isFallFlying.get()\n                ? false\n                : !(this.isBaby.isPresent() && entity instanceof LivingEntity living) || living.isBaby() == this.isBaby.get();',
            'if (this.isFallFlying.isPresent() && entity instanceof LivingEntity fallLiving && fallLiving.isFallFlying() != this.isFallFlying.get()) {\n            return false;\n        }\n        if (this.isBaby.isPresent() && entity instanceof LivingEntity babyLiving && babyLiving.isBaby() == this.isBaby.get()) {\n            return false;\n        }\n        return true;'
        )

    # --- 13. getMapColor(BlockState, LevelReader, BlockPos, MapColor) callers ---
    # Decompiler produced 2-arg calls; the method needs (state, level, pos, default).
    if 'FallingDustParticle' in filename:
        content = content.replace(
            'tintColor = blockState.getMapColor(level, pos).col;',
            'tintColor = blockState.getMapColor(blockState, level, pos, MapColor.NONE).col;'
        )
        if 'import net.minecraft.world.level.material.MapColor;' not in content:
            content = content.replace(
                'import net.minecraft.world.level.block.state.BlockState;',
                'import net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.material.MapColor;'
            )
    if 'MapItem' in filename:
        content = content.replace(
            'Blocks.DIRT.defaultBlockState().getMapColor(level, BlockPos.ZERO)',
            'Blocks.DIRT.defaultBlockState().getMapColor(Blocks.DIRT.defaultBlockState(), level, BlockPos.ZERO, MapColor.NONE)'
        )
        content = content.replace(
            'Blocks.STONE.defaultBlockState().getMapColor(level, BlockPos.ZERO)',
            'Blocks.STONE.defaultBlockState().getMapColor(Blocks.STONE.defaultBlockState(), level, BlockPos.ZERO, MapColor.NONE)'
        )
        content = content.replace(
            'state.getMapColor(level, blockPos) == MapColor.NONE',
            'state.getMapColor(state, level, blockPos, MapColor.NONE) == MapColor.NONE'
        )
        content = content.replace(
            'colorCount.add(state.getMapColor(level, blockPos));',
            'colorCount.add(state.getMapColor(state, level, blockPos, MapColor.NONE));'
        )
    if 'AnvilBlock' in filename or 'ConcretePowderBlock' in filename:
        content = content.replace(
            'return blockState.getMapColor(level, pos).col;',
            'return blockState.getMapColor(blockState, (LevelReader) level, pos, MapColor.NONE).col;'
        )
        # idempotency: also fix a previously-applied uncast version
        content = content.replace(
            'return blockState.getMapColor(blockState, level, pos, MapColor.NONE).col;',
            'return blockState.getMapColor(blockState, (LevelReader) level, pos, MapColor.NONE).col;'
        )
        if 'import net.minecraft.world.level.material.MapColor;' not in content:
            content = content.replace(
                'import net.minecraft.world.level.block.state.BlockState;',
                'import net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.material.MapColor;'
            )
        if 'import net.minecraft.world.level.LevelReader;' not in content:
            content = content.replace(
                'import net.minecraft.world.level.block.state.BlockState;',
                'import net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.level.LevelReader;'
            )

    # --- 14. shouldRenderFace: 3-arg static (state, neighborState, direction) ---
    if 'ModelBlockRenderer' in filename:
        content = content.replace(
            'Block.shouldRenderFace(level, pos, state, neighborState, direction)',
            'Block.shouldRenderFace(state, neighborState, direction)'
        )
    if 'TheEndGatewayBlockEntity' in filename:
        content = content.replace(
            'Block.shouldRenderFace(this.level, this.worldPosition, this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(direction)), direction)',
            'Block.shouldRenderFace(this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(direction)), direction)'
        )

    # --- 15. Block classes: @Override on non-overriding (NeoForge patch-in) methods ---
    if 'ComparatorBlock' in filename:
        content = content.replace(
            '@Override\n    public boolean getWeakChanges(',
            'public boolean getWeakChanges('
        )
        content = content.replace(
            '@Override\n    public void onNeighborChange(',
            'public void onNeighborChange('
        )
    if 'DropExperienceBlock' in filename or 'RedStoneOreBlock' in filename or 'SculkCatalystBlock' in filename \
            or 'SculkSensorBlock' in filename or 'SculkShriekerBlock' in filename or 'SpawnerBlock' in filename:
        content = content.replace(
            '@Override\n    public int getExpDrop(',
            'public int getExpDrop('
        )
    if 'TntBlock' in filename:
        content = content.replace(
            '@Override\n    public boolean onCaughtFire(',
            'public boolean onCaughtFire('
        )
    if 'TrapDoorBlock' in filename:
        content = content.replace(
            '@Override\n    public boolean isLadder(',
            'public boolean isLadder('
        )
        content = content.replace(
            'return down.getBlock().makesOpenTrapdoorAboveClimbable(down, world, downPos, state);',
            'return down.getBlock() instanceof LadderBlock;'
        )
    # NeoForge extension methods that are not overrides
    for block_class in ['DoorBlock', 'DoublePlantBlock', 'BedBlock', 'PistonBaseBlock', 'PistonHeadBlock']:
        if block_class in filename:
            content = content.replace(
                '@Override\n    public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(',
                'public net.neoforged.neoforge.common.util.BlockRelocability getRelocability('
            )
            content = content.replace(
                '@Override\n    public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {',
                'public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {'
            )
    if 'PistonBaseBlock' in filename:
        content = content.replace(
            'super.rotate(state, world, pos, direction)',
            'super.rotate(state, direction)'
        )

    # --- 16. LevelEventHandler: variable redefinition in case 2003 ---
    if 'LevelEventHandler' in filename:
        content = content.replace(
            'case 2003:\n                double x = pos.getX() + 0.5;\n                double y = pos.getY();\n                double z = pos.getZ() + 0.5;\n                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);',
            'case 2003:\n                double x2003 = pos.getX() + 0.5;\n                double y2003 = pos.getY();\n                double z2003 = pos.getZ() + 0.5;\n                ItemParticleOption breakParticle2003 = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);'
        )
        content = content.replace(
            'this.level.addParticle(breakParticle, x, y, z, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);',
            'this.level.addParticle(breakParticle2003, x2003, y2003, z2003, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);'
        )
        # Portal particle loop still references the renamed locals
        content = content.replace(
            'x + Math.cos(angle) * 5.0',
            'x2003 + Math.cos(angle) * 5.0'
        )
        content = content.replace(
            'y - 0.4',
            'y2003 - 0.4'
        )
        content = content.replace(
            'z + Math.sin(angle) * 5.0',
            'z2003 + Math.sin(angle) * 5.0'
        )

    # --- 17. SpawnPlacements: generic type mismatch ---
    if 'SpawnPlacements' in filename:
        content = content.replace(
            'return data == null || data.predicate.test(type, level, spawnReason, pos, random);',
            '@SuppressWarnings("unchecked")\n        SpawnPlacements.SpawnPredicate raw = (SpawnPlacements.SpawnPredicate) data.predicate;\n        return data == null || raw.test(type, level, spawnReason, pos, random);'
        )

    if content != original:
        with open(path, 'w') as fh:
            fh.write(content)
        return True
    return False


for root, dirs, files in os.walk(base):
    for f in files:
        if not f.endswith('.java'):
            continue
        path = os.path.join(root, f)
        if fix_file(path, f):
            fixed_files += 1

print(f"Fixed {fixed_files} files")
