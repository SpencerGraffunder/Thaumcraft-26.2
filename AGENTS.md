### Definition of Done
**Never declare work complete until ALL of the following are true:**
1. `./gradlew build` exits 0 (zero compile errors)
2. `grep -r "TODO" src/ --include="*.java" | wc -l` returns 0
3. Every finding in the audit has a working implementation (not a stub or comment placeholder)
4. The final VERIFY task in the todo is marked done — and only after the above pass

**Stopping is ONLY allowed when there is literally nothing you can do without the user's answer.**
"I finished a batch" is NOT a reason to stop. "I hit a natural boundary" is NOT a reason to stop.
If the build fails, fix it and re-run. If TODOs remain, implement them. If the audit has gaps, fill them.
Keep going until the Definition of Done is met. Do not summarize, report, or ask for permission to continue.

### Document at every stopping point
Keep the documents updated for handing off to other agents. To do that:
Update the todo whenever there's something new that comes up or you finish something that was on it.
Commit whenever done with a change, even minor.
Update the readme, commit, and push whenever done with a significant change.

### Utilize subagents
Token usage is free, spin up subagents to keep context clean. Handoff tasks by documenting them in the todo then instructing the subagent to just work on that one task.

### Give status updates periodically
To keep the user up to date, provide simple easy to read statuses with the original prompt in mind.

### Minecraft client/server GUI
To launch, verify, screenshot, or drive the in-game client (or connect it to the dev
server) on this box, follow `skills/minecraft-gui/SKILL.md`. Key points: most verification is
log-based (`CI=true ./gradlew runClient` then grep the log) and needs **no** GUI input; GUI
input on this Wayland box goes through the synthetic-hardware drivers in `tools/`
(`ucursor.py`, `evdev_input.py`), not the `computer` tool (which is focus-only and can't see
the Modrinth App webview).