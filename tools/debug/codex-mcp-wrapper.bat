@echo off
setlocal
for %%I in ("%~dp0..\..") do set "ROOT=%%~fI"
set LOG_FILE=%ROOT%\build\codex-mcp-wrapper.log
set STDERR_LOG=%ROOT%\build\codex-mcp-stderr.log

>>"%LOG_FILE%" echo [%DATE% %TIME%] start
>>"%LOG_FILE%" echo [%DATE% %TIME%] cwd=%CD%
>>"%LOG_FILE%" echo [%DATE% %TIME%] JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS%

if not defined JAVA_TOOL_OPTIONS set "JAVA_TOOL_OPTIONS=-Dmoddevmcp.host=127.0.0.1 -Dmoddevmcp.port=47653"

if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Zulu\zulu-21\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

if exist "%ROOT%\Server\build\classes\java\main" (
    set "CP=%ROOT%\Server\build\classes\java\main"
) else (
    set "CP=%ROOT%\Server\build\libs\Server.jar"
)
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\io.modelcontextprotocol.sdk\mcp-core\1.0.0\e9e0b7023f0dc5d4527909dffa0f3ac7ff9faf3a\mcp-core-1.0.0.jar"
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\com.google.code.gson\gson\2.10.1\b3add478d4382b78ea20b1671390a858002feb6c\gson-2.10.1.jar"
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.16\172931663a09a1fa515567af5fbef00897d3c04\slf4j-api-2.0.16.jar"
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\io.projectreactor\reactor-core\3.7.0\e98fd1c48144d43f48141b9ebd6723da3b88fb77\reactor-core-3.7.0.jar"
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\org.reactivestreams\reactive-streams\1.0.4\3864a1320d97d7b045f729a326e1e077661f31b7\reactive-streams-1.0.4.jar"
set "CP=%CP%;%ROOT%\.gradle-user\caches\modules-2\files-2.1\com.fasterxml.jackson.core\jackson-annotations\2.20\6a5e7291ea3f2b590a7ce400adb7b3aea4d7e12c\jackson-annotations-2.20.jar"

"%JAVA_EXE%" -cp "%CP%" dev.vfyjxf.mcp.server.bootstrap.ModDevMcpStdioMain 2>>"%STDERR_LOG%"
set "EXIT_CODE=%ERRORLEVEL%"

>>"%LOG_FILE%" echo [%DATE% %TIME%] exit=%EXIT_CODE%
exit /b %EXIT_CODE%
