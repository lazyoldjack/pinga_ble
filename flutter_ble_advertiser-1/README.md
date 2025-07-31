# Flutter BLE Advertiser

This Flutter application demonstrates how to advertise a phone using Bluetooth Low Energy (BLE). It provides a simple user interface to start and stop advertising, and displays the current advertising status.

## Project Structure

```
flutter_ble_advertiser
├── lib
│   ├── main.dart
│   ├── ble
│   │   ├── ble_advertiser.dart
│   │   └── ble_constants.dart
│   └── widgets
│       └── advertiser_status.dart
├── pubspec.yaml
└── README.md
```

## Setup Instructions

1. **Clone the repository:**
   ```
   git clone <repository-url>
   cd flutter_ble_advertiser
   ```

2. **Install dependencies:**
   Make sure you have Flutter installed on your machine. Run the following command to install the required dependencies:
   ```
   flutter pub get
   ```

3. **Run the application:**
   Connect your device or start an emulator, then run:
   ```
   flutter run
   ```

## Usage Guidelines

- The main screen will display the current status of BLE advertising.
- Press the button to start or stop advertising.
- Ensure that your device has Bluetooth enabled and the necessary permissions granted.

## Dependencies

This project uses the following dependencies:

- `flutter_blue`: A Flutter plugin for Bluetooth Low Energy (BLE) communication.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue for any suggestions or improvements.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.