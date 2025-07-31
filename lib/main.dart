import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BLE Advertiser',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blueAccent),
        useMaterial3: true,
        fontFamily: 'Inter', // Using Inter font as per instructions
      ),
      home: const BleAdvertiserScreen(),
    );
  }
}

class BleAdvertiserScreen extends StatefulWidget {
  const BleAdvertiserScreen({super.key});

  @override
  State<BleAdvertiserScreen> createState() => _BleAdvertiserScreenState();
}

class _BleAdvertiserScreenState extends State<BleAdvertiserScreen> {
  // MethodChannel for communication with native Android code
static const MethodChannel _channel = MethodChannel('com.example.pinga_ble/ble');
  bool _isAdvertising = false;
  String _statusMessage = "Not advertising";
  int _selectedPowerLevelIndex = 1; // Default to ADVERTISE_TX_POWER_LOW

  // Map integer index to human-readable string and native constant value
  final List<Map<String, dynamic>> _powerLevels = [
    {'name': 'Ultra Low', 'value': 0}, // ADVERTISE_TX_POWER_ULTRA_LOW
    {'name': 'Low', 'value': 1},      // ADVERTISE_TX_POWER_LOW
    {'name': 'Medium', 'value': 2},   // ADVERTISE_TX_POWER_MEDIUM
    {'name': 'High', 'value': 3},     // ADVERTISE_TX_POWER_HIGH
  ];

  @override
  void initState() {
    super.initState();
    // Set up method call handler to receive updates from native side
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onAdvertisingStatusChanged':
        setState(() {
          _isAdvertising = call.arguments['isAdvertising'];
          _statusMessage = call.arguments['message'];
        });
        break;
      default:
        debugPrint('Unknown method ${call.method}');
    }
  }

  Future<void> _toggleAdvertising() async {
    try {
      if (_isAdvertising) {
        await _channel.invokeMethod('stopAdvertising');
      } else {
        await _channel.invokeMethod('startAdvertising', {
          'txPowerLevel': _powerLevels[_selectedPowerLevelIndex]['value'],
        });
      }
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = "Error: ${e.message}";
      });
      _showMessageBox("Error", "Failed to toggle advertising: ${e.message}");
    }
  }

  Future<void> _setPowerLevel(int index) async {
    setState(() {
      _selectedPowerLevelIndex = index;
    });
    if (_isAdvertising) {
      try {
        await _channel.invokeMethod('updateAdvertisingPowerLevel', {
          'txPowerLevel': _powerLevels[_selectedPowerLevelIndex]['value'],
        });
        setState(() {
          _statusMessage = "Power level updated to ${_powerLevels[_selectedPowerLevelIndex]['name']}";
        });
      } on PlatformException catch (e) {
        setState(() {
          _statusMessage = "Error updating power: ${e.message}";
        });
        _showMessageBox("Error", "Failed to update power level: ${e.message}");
      }
    }
  }

  void _showMessageBox(String title, String message) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15.0)),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
          content: Text(message),
          actions: <Widget>[
            TextButton(
              child: const Text('OK', style: TextStyle(color: Colors.blueAccent)),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('BLE Advertiser', style: TextStyle(color: Colors.white)),
        backgroundColor: Colors.blueAccent,
        elevation: 4,
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
                  // Branding Image
            Image.asset(
              'assets/Twifik3.png',
              height: 50, // Adjust size as needed
            ),
            const SizedBox(height: 8),
            InkWell(
              onTap: () async {
                final url = Uri.parse('https://twifik.com');
                //if (await canLaunchUrl(url)) {
                  await launchUrl(url, mode: LaunchMode.platformDefault);
                //}
              },
              child: Text(
                'twifik.com',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.blueAccent,
                  decoration: TextDecoration.underline,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),            
            const SizedBox(height: 20),
            // Status Display
            Container(
              padding: const EdgeInsets.all(16.0),
              margin: const EdgeInsets.only(bottom: 20.0),
              decoration: BoxDecoration(
                color: _isAdvertising ? Colors.green.shade100 : Colors.red.shade100,
                borderRadius: BorderRadius.circular(15.0),
                border: Border.all(
                  color: _isAdvertising ? Colors.green.shade400 : Colors.red.shade400,
                  width: 2,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.grey.withOpacity(0.3),
                    spreadRadius: 2,
                    blurRadius: 5,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Text(
                    _isAdvertising ? 'ADVERTISING' : 'IDLE',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: _isAdvertising ? Colors.green.shade800 : Colors.red.shade800,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    _statusMessage,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 16,
                      color: _isAdvertising ? Colors.green.shade700 : Colors.red.shade700,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 30),

            // Toggle Advertising Button
            ElevatedButton(
              onPressed: _toggleAdvertising,
              style: ElevatedButton.styleFrom(
                backgroundColor: _isAdvertising ? Colors.redAccent : Colors.blueAccent,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15.0),
                ),
                elevation: 5,
              ),
              child: Text(
                _isAdvertising ? 'Stop Advertising' : 'Start Advertising',
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ),

            const SizedBox(height: 30),

            // Power Level Control
            Text(
              'BLE Power Level: ${_powerLevels[_selectedPowerLevelIndex]['name']}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            Slider(
              value: _selectedPowerLevelIndex.toDouble(),
              min: 0,
              max: (_powerLevels.length - 1).toDouble(),
              divisions: _powerLevels.length - 1,
              label: _powerLevels[_selectedPowerLevelIndex]['name'],
              onChanged: (double newValue) {
                _setPowerLevel(newValue.round());
              },
              activeColor: Colors.blueAccent,
              inactiveColor: Colors.blueAccent.withOpacity(0.3),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: _powerLevels.map((level) => Text(level['name'])).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

