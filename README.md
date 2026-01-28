# 🎵 JDA Lavalink Music Bot

A lightweight, high-quality Discord music bot written in **Java** using [JDA (Java Discord API)](https://github.com/DV8tion/JDA) and the [Lavalink Client](https://github.com/devoxin/lavalink-client).

This bot supports Slash Commands, YouTube searching/playing via URL, and utilizes an external Lavalink server for stable audio streaming.

## ✨ Features

* **Slash Commands**: Modern interaction using `/` commands.
* **High-Quality Audio**: Powered by Lavalink for lag-free music playback.
* **Search & Play**: Supports YouTube links (`http...`) and keyword searches (`ytsearch:`).
* **Playback Controls**: Play, Pause, Resume, and Stop.
* **Fun Interactions**: Responds with a custom message when the bot is mentioned/tagged.

## 🛠️ Prerequisites

Before running the bot, ensure you have the following installed:

* **Java Development Kit (JDK)**: Version **17** or higher.
* **Maven**: For dependency management and building the project.
* **Lavalink Server**: A running instance of [Lavalink](https://github.com/lavalink-devs/Lavalink).
* **Discord Bot Token**: Obtained from the [Discord Developer Portal](https://discord.com/developers/applications).

## ⚙️ Installation & Setup

### 1. Clone the Repository
Clone this repository to your local machine.

### 2. Configure Environment Variables
Create a file named `.env` in the root directory of your project. Add the following configuration (make sure these match your Lavalink `application.yml`):

```properties
# Discord Bot Token
TOKEN=your_discord_bot_token_here

# Lavalink Server Connection Info
LAVALINK_URI=http://localhost:2333
LAVALINK_PASSWORD=youshallnotpass
```

### 3. Start Lavalink
Make sure your Lavalink server is running before starting the bot:
```bash
java -jar Lavalink.jar
```

### 4. Build and Run (Using Maven)
Since this project uses Maven (`pom.xml`), run the following commands:

**Compile and package:**
```bash
mvn clean package
```

**Run the bot:**
After packaging, run the generated JAR file (usually found in the `target/` folder):
```bash
# Replace 'your-bot-name.jar' with the actual filename inside target/
java -jar target/your-bot-name-1.0-SNAPSHOT.jar
```

## 🎮 Commands

| Command | Description | Example |
| :--- | :--- | :--- |
| `/play <query>` | Play music via URL or Search Keywords | `/play query:lofi hip hop` |
| `/pause` | Pause the current track | `/pause` |
| `/resume` | Resume playback | `/resume` |
| `/stop` | Stop playback and leave the channel | `/stop` |
| `/ping` | Check bot latency | `/ping` |

## 📝 Important Notes

1.  **Gateway Intents**:
    Ensure you have enabled the following **Privileged Intents** in the Discord Developer Portal:
    * **Message Content Intent** (Required for the `onMessageReceived` easter egg).
    * **Server Members Intent**.

2.  **Lavalink Version**:
    Ensure your `LAVALINK_URI` points to a valid Lavalink v4 (or compatible) node.

3.  **Dependencies**:
    This project relies on `JDA`, `lavalink-client`, and `dotenv-java`. These are managed automatically via your `pom.xml`.
