# Alcedo Studio Privacy Policy

**Last updated: 2026-07-21**

Alcedo Studio is a desktop image editor built with privacy as a core principle. This document describes what data we collect, how we use it, and the choices you have.

## 1. Data We Collect

### 1.1 Data We Do NOT Collect

Alcedo Studio does **not** collect, transmit, or store any of the following on remote servers:

- Personal information (name, email, phone number)
- Usage analytics or telemetry
- Crash reports (unless you explicitly choose to submit one)
- Device identifiers or fingerprinting data
- Location data
- IP addresses

### 1.2 Data Stored Locally

The following data is stored **only on your local machine**:

| Data | Storage Location | Purpose |
|------|-----------------|---------|
| Project database | User-specified directory | Image metadata, edit history, ratings |
| AI provider API keys | OS credential store (Windows Credential Manager / macOS Keychain / Linux encrypted file) | AI analysis features |
| Application settings | OS settings registry (QSettings) | Preferences, theme, language |
| Thumbnail cache | Local cache directory | Faster thumbnail display |
| Crash logs | Local temp directory | Debugging (never sent automatically) |

### 1.3 AI Provider Credentials

When you configure an AI provider (e.g., OpenAI, Google Gemini), your API key is stored exclusively in the operating system's native credential store:

- **Windows**: Windows Credential Manager (CredAPI/DPAPI encrypted)
- **macOS**: Keychain Services (Data Protection keychain)
- **Linux**: AES-256 encrypted file in `~/.config/AlcedoStudio/AiCredentialStore/` with owner-only permissions (0600)

API keys are **never** stored in plaintext configuration files, logs, or transmitted to any server other than the AI provider you explicitly configured.

## 2. Network Communications

### 2.1 Update Checks

When automatic update checking is enabled (and can be disabled in Settings), Alcedo Studio connects to `https://api.github.com/repos/<owner>/<repo>/releases/latest` to check for new versions.

- **Protocol**: HTTPS with TLS 1.2+ and certificate verification
- **Data sent**: Only a User-Agent header containing the application version
- **Data received**: Latest release version number, release notes, and download URL
- **Frequency**: Configurable (default: once every 24 hours)
- **Opt-out**: Settings → Advanced → Disable "Check for updates automatically"

### 2.2 AI Provider Communication

When you use AI features (image description, rating, analysis), your image data and prompts are sent **directly** to the AI provider you configured. Alcedo Studio acts as a client between you and the provider.

- **No proxy**: Data goes directly from your machine to the AI provider's API
- **HTTPS required**: All AI provider API communication uses HTTPS
- **Your responsibility**: Review the AI provider's privacy policy for how they handle your data

### 2.3 No Other Network Activity

Alcedo Studio does not make any other network connections. There is no telemetry, no analytics, no advertising, and no tracking of any kind.

## 3. Local AI / Sidecar Runtime

Alcedo Studio can run AI models locally via a sidecar runtime process. When using local AI:

- All inference runs entirely on your machine
- No data is sent to any remote server
- Model files are downloaded from the project's release repository over HTTPS and stored locally
- Model downloads require explicit user action

## 4. Third-Party Services

Alcedo Studio integrates with the following third-party services **only when you explicitly configure them**:

| Service | Purpose | Data Shared |
|---------|---------|-------------|
| OpenAI API | Image analysis, description | Image data, text prompts |
| Google Gemini API | Image analysis, description | Image data, text prompts |
| Other OpenAI-compatible APIs | Image analysis | Image data, text prompts |
| GitHub Releases API | Update checking | Application version (in User-Agent header) |

We do not share any data with advertising networks, data brokers, or analytics providers.

## 5. Data Retention

- **Project data**: Retained until you delete the project or uninstall the application
- **AI credentials**: Retained in the OS credential store until you remove them from Settings
- **Cache data**: Can be cleared at any time from Settings → Cache → Clear Cache
- **Crash logs**: Retained in the local temp directory until cleared by the OS or manually deleted

## 6. Your Rights

You have complete control over your data:

- **Access**: All data is stored locally in standard formats (DuckDB, JSON)
- **Deletion**: Delete projects, credentials, and cache through the application UI
- **Export**: Export your images and edit history at any time
- **Opt-out**: Disable update checks and AI features at any time in Settings
- **Transparency**: This application is open-source; you can audit all network code

## 7. Children's Privacy

Alcedo Studio does not knowingly collect personal information from children under 13. Since we do not collect any personal information from anyone, our service is equally available to users of all ages.

## 8. Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected in the "Last updated" date at the top of this document. As an open-source project, all changes are publicly visible in our version control history.

## 9. Contact

For privacy-related questions or concerns, please open an issue on our GitHub repository or contact the project maintainers directly.

## 10. Open Source License

Alcedo Studio is licensed under GPL-3.0-only. You have the right to inspect, modify, and redistribute the source code, including all privacy-relevant functionality.
