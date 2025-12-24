# Account Manager Plugin for RuneLite

A RuneLite plugin for securely managing multiple OSRS account credentials with encrypted storage and optional TOTP (2FA) support.

## Features

- **Encrypted Storage**: Credentials encrypted with AES-256-GCM
- **Master Password**: Single password to unlock all saved accounts
- **TOTP Support**: Optional 2FA secret storage for automatic code generation
- **One-Click Login**: Fill credentials with a single click on the login screen
- **Auto-Lock**: Configurable timeout to automatically lock the vault

## Installation

1. Build the plugin: `./gradlew build`
2. Copy the JAR from `build/libs/` to your RuneLite plugins folder
3. Enable "Account Manager" in RuneLite's plugin configuration

## Usage

1. Click the Account Manager icon in the sidebar
2. Create a master password (first time only)
3. Add accounts with nickname, username, password, and optional TOTP secret
4. On the login screen, click an account to fill credentials
5. Click the "Login" button to log in

## Security

| Component | Implementation |
|-----------|----------------|
| Encryption | AES-256-GCM with 128-bit authentication tag |
| Key Derivation | PBKDF2-HMAC-SHA256, 310,000 iterations |
| Salt | Unique random 32-byte salt per installation |
| Storage | RuneLite's encrypted settings.properties |

Credentials are only decrypted in memory when needed and are never logged or stored in plaintext.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| Auto-fill OTP | On | Automatically fill OTP when authenticator screen appears |
| Auto-lock after | 0 (disabled) | Lock vault after specified minutes of inactivity |

## Building

```bash
# Build
./gradlew build

# Run with RuneLite
./gradlew run

# Run tests
./gradlew test
```

## Compliance

This plugin is designed to comply with:
- Jagex Third-Party Client Guidelines
- RuneLite Plugin Guidelines

No automation or input simulation is performed, only using PASTE functionality provided by RuneLite. Users must click the login button themselves.

## License

BSD 2-Clause License - see [LICENSE](LICENSE) file.
