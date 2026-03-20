@echo off
set "GRADLE_USER_HOME=D:\ProjectDir\AgentFarm\ModDevMCP\.gradle-user"
set "JAVA_TOOL_OPTIONS=-Dmaven.repo.local=D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec\.m2-local\.m2\repository"
call gradlew.bat runClient --no-daemon --stacktrace --info > D:\ProjectDir\AgentFarm\ModDevMCP\.worktrees\http-skill-bigbang-exec\TestMod\build\moddevmcp\runClient.cmd.log 2>&1