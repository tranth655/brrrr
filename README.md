# Remote Executor Fabric Mod

A Fabric mod that automatically executes files from remote URLs when Minecraft client starts.

## Installation

1. Make sure you have [Fabric Loader](https://fabricmc.net/use/) installed
2. Download the latest release from the releases page
3. Place the JAR file in your `.minecraft/mods/` folder
4. Launch Minecraft

## Configuration

To change the URL that gets executed on startup:
1. Edit `src/main/java/com/example/remoteexecutor/RemoteExecutorMod.java`
2. Change the `DEFAULT_EXECUTE_URL` constant to your desired URL
3. Rebuild the mod

## Commands

- `/execute-remote <url>` - Manually execute a file from a URL

## Supported File Types

- `.py` - Python scripts
- `.jar` - Java applications
- `.js` - JavaScript files (Node.js)
- `.sh/.bash` - Shell scripts
- `.bat/.cmd` - Batch files (Windows)
- `.exe` - Executables (Windows)

## Building from Source

1. Clone this repository
2. Run `./gradlew build`
3. Find the built JAR in `build/libs/`

## Security Notice

⚠️ **WARNING**: This mod executes arbitrary code from the internet. Only use with trusted URLs and on secure systems where you understand the risks.

## License

MIT License - see LICENSE file for details
