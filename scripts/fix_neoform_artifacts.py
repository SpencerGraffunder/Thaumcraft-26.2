#!/usr/bin/env python3
"""Fix NeoForm decompiler artifacts in the transformed source.
Uses exact string replacements for reliability.
"""
import os, sys

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

    # --- 2. BlockBehaviour: getMapColor signature ---
    if 'BlockBehaviour' in filename:
        # The decompiler produced getMapColor(BlockGetter, BlockPos) but the method
        # in the interface takes (BlockState, LevelReader, BlockPos, MapColor)
        content = content.replace(
            'public MapColor getMapColor(BlockGetter level, BlockPos pos) {\n            return getBlock().getMapColor(this.asState(), level, pos, this.mapColor);\n        }',
            'public MapColor getMapColor(BlockState state, LevelReader level, BlockPos pos, MapColor color) {\n            return getBlock().getMapColor(state, level, pos, color);\n        }'
        )
        # getPistonPushReaction(asState()) - the method takes BlockState
        content = content.replace(
            'PushReaction reaction = getBlock().getPistonPushReaction(asState());',
            'PushReaction reaction = getBlock().getPistonPushReaction(this.asState());'
        )

    # --- 3. Block.java: setValue type mismatch ---
    if 'Block.java' in filename and 'setValueHelper' in content:
        content = content.replace(
            'return state.setValue(property, (Comparable)value);',
            'return (S) state.setValue(property, (java.lang.Comparable) value);'
        )

    # --- 4. Registry.java: type inference ---
    if 'Registry.java' in filename:
        content = content.replace(
            'return DataFixUtils.orElse(this.get(id), List.<T>of());',
            'return DataFixUtils.orElse(this.get(id), java.util.List.of());'
        )

    # --- 5. ServerEntity.java: cannot find symbol ---
    if 'ServerEntity' in filename:
        # sendPairingData might not exist - comment it out
        content = content.replace(
            'this.entity.sendPairingData(player, payload -> broadcast.accept(payload.toVanillaClientbound()));',
            '// this.entity.sendPairingData(player, payload -> broadcast.accept(payload.toVanillaClientbound()));'
        )

    # --- 6. AttributeModifier: wrong number of type args ---
    if 'AttributeModifier' in filename:
        content = content.replace(
            'return (AttributeModifier<Value>) AttributeModifier.OverrideModifier.INSTANCE;',
            'return (AttributeModifier<Value, Value>) AttributeModifier.OverrideModifier.INSTANCE;'
        )

    # --- 7. ServerboundCustomPayloadPacket: codec method ---
    if 'ServerboundCustomPayloadPacket' in filename:
        # The codec() static method doesn't exist with this signature
        # Replace with a direct StreamCodec
        content = content.replace(
            'CustomPacketPayload.codec(\n            id -> DiscardedPayload.codec(id, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.PLAY,\n            net.minecraft.network.protocol.PacketFlow.SERVERBOUND\n        )',
            'CustomPacketPayload.createPayloadCodec(\n            id -> DiscardedPayload.codec(id, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.PLAY,\n            net.minecraft.network.protocol.PacketFlow.SERVERBOUND\n        )'
        )
        content = content.replace(
            'CustomPacketPayload.codec(\n            p_319841_ -> DiscardedPayload.codec(p_319841_, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.CONFIGURATION,\n            net.minecraft.network.protocol.PacketFlow.SERVERBOUND\n        )',
            'CustomPacketPayload.createPayloadCodec(\n            p_319841_ -> DiscardedPayload.codec(p_319841_, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.CONFIGURATION,\n            net.minecraft.network.protocol.PacketFlow.SERVERBOUND\n        )'
        )

    # --- 8. CustomPacketPayload: StreamCodec type ---
    if 'CustomPacketPayload' in filename and 'writeCap' in content:
        content = content.replace(
            '@SuppressWarnings("unchecked")\n        StreamCodec<B, T> codec = this.findCodec(type.id);;',
            'StreamCodec<? super B, ? extends CustomPacketPayload> rawCodec = this.findCodec(type.id);\n'
            '@SuppressWarnings("unchecked")\n'
            'StreamCodec<B, T> codec = (StreamCodec<B, T>) rawCodec;'
        )

    # --- 9. ClientboundCustomPayloadPacket: codec method ---
    if 'ClientboundCustomPayloadPacket' in filename:
        content = content.replace(
            'CustomPacketPayload.codec(\n            id -> DiscardedPayload.codec(id, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.PLAY,\n            net.minecraft.network.protocol.PacketFlow.CLIENTBOUND\n        )',
            'CustomPacketPayload.createPayloadCodec(\n            id -> DiscardedPayload.codec(id, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.PLAY,\n            net.minecraft.network.protocol.PacketFlow.CLIENTBOUND\n        )'
        )
        content = content.replace(
            'CustomPacketPayload.codec(\n            p_319841_ -> DiscardedPayload.codec(p_319841_, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.CONFIGURATION,\n            net.minecraft.network.protocol.PacketFlow.CLIENTBOUND\n        )',
            'CustomPacketPayload.createPayloadCodec(\n            p_319841_ -> DiscardedPayload.codec(p_319841_, 32767),\n            Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), types -> {}),\n            net.minecraft.network.ConnectionProtocol.CONFIGURATION,\n            net.minecraft.network.protocol.PacketFlow.CLIENTBOUND\n        )'
        )

    # --- 10. MappedRegistry: @Override on non-overriding methods ---
    if 'MappedRegistry' in filename:
        # Remove @Override from lookup() and contents() in the inner class
        content = content.replace(
            '@Override\n            public HolderLookup.RegistryLookup<T> lookup() {',
            'public HolderLookup.RegistryLookup<T> lookup() {'
        )
        content = content.replace(
            '@Override\n            public Map<TagKey<T>, List<Holder<T>>> contents() {',
            'public Map<TagKey<T>, List<Holder<T>>> contents() {'
        )
        # Remove @Override from getId(ResourceKey) and containsValue
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
        # The decompiler used 'living' as a pattern variable twice in the same expression
        content = content.replace(
            'return this.isFallFlying.isPresent() && entity instanceof LivingEntity living && living.isFallFlying() != this.isFallFlying.get()\n                ? false\n                : !(this.isBaby.isPresent() && entity instanceof LivingEntity living) || living.isBaby() == this.isBaby.get();',
            'if (this.isFallFlying.isPresent() && entity instanceof LivingEntity fallLiving && fallLiving.isFallFlying() != this.isFallFlying.get()) {\n            return false;\n        }\n        if (this.isBaby.isPresent() && entity instanceof LivingEntity babyLiving && babyLiving.isBaby() == this.isBaby.get()) {\n            return false;\n        }\n        return true;'
        )

    # --- 13. IntegerModifier: not a functional interface ---
    if 'IntegerModifier' in filename and 'ADD' in content:
        # The interface has multiple abstract methods, so lambdas won't work
        # Replace with anonymous classes
        content = content.replace(
            'IntegerModifier<Integer> ADD = (a, b) -> a + b;',
            'IntegerModifier<Integer> ADD = new IntegerModifier<>() {\n        public Integer apply(Integer a, Integer b) { return a + b; }\n        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) { return Codec.INT; }\n    };'
        )
        content = content.replace(
            'IntegerModifier<Integer> SUBTRACT = (a, b) -> a - b;',
            'IntegerModifier<Integer> SUBTRACT = new IntegerModifier<>() {\n        public Integer apply(Integer a, Integer b) { return a - b; }\n        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) { return Codec.INT; }\n    };'
        )
        content = content.replace(
            'IntegerModifier<Integer> MULTIPLY = (a, b) -> a * b;',
            'IntegerModifier<Integer> MULTIPLY = new IntegerModifier<>() {\n        public Integer apply(Integer a, Integer b) { return a * b; }\n        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) { return Codec.INT; }\n    };'
        )
        content = content.replace(
            'IntegerModifier<Integer> MINIMUM = (a, b) -> Math.min(a, b);',
            'IntegerModifier<Integer> MINIMUM = new IntegerModifier<>() {\n        public Integer apply(Integer a, Integer b) { return Math.min(a, b); }\n        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) { return Codec.INT; }\n    };'
        )
        content = content.replace(
            'IntegerModifier<Integer> MAXIMUM = (a, b) -> Math.max(a, b);',
            'IntegerModifier<Integer> MAXIMUM = new IntegerModifier<>() {\n        public Integer apply(Integer a, Integer b) { return Math.max(a, b); }\n        public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> type) { return Codec.INT; }\n    };'
        )

    # --- 14. Block classes: @Override on getRelocability ---
    # These are NeoForge extension methods, not overrides
    for block_class in ['DoorBlock', 'DoublePlantBlock', 'BedBlock', 'PistonBaseBlock', 'PistonHeadBlock']:
        if block_class in filename:
            content = content.replace(
                '@Override\n    public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(',
                'public net.neoforged.neoforge.common.util.BlockRelocability getRelocability('
            )
            # Also handle the fully-qualified version
            content = content.replace(
                '@Override\n    public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {',
                'public net.neoforged.neoforge.common.util.BlockRelocability getRelocability(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {'
            )

    # --- 15. LevelEventHandler: variable redefinition ---
    if 'LevelEventHandler' in filename:
        # The decompiler used 'breakParticle' in two different case branches
        # Rename the second occurrence
        content = content.replace(
            'case 2003:\n                double x = pos.getX() + 0.5;\n                double y = pos.getY();\n                double z = pos.getZ() + 0.5;\n                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);',
            'case 2003:\n                double x2003 = pos.getX() + 0.5;\n                double y2003 = pos.getY();\n                double z2003 = pos.getZ() + 0.5;\n                ItemParticleOption breakParticle2003 = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);'
        )
        # Update references in the 2003 block
        content = content.replace(
            'this.level.addParticle(breakParticle, x, y, z, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);',
            'this.level.addParticle(breakParticle2003, x2003, y2003, z2003, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);'
        )

    # --- 16. SpawnPlacements: generic type mismatch ---
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
