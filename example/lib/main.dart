import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_citizen_printer/flutter_citizen_printer.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String selectedAddress = "192.168.0.100";

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Citizen Printer Example')),
        body: Builder(builder: (context) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton(
                  onPressed: () async {
                    try {
                      final ips = await FlutterCitizenPrinter.detectPrinters();
                      log("detected ips: $ips");
                      if (context.mounted) {
                        showDialog(
                          context: context,
                          builder: (_) => AlertDialog(
                            title: Text('Found printers'),
                            content: ListView.builder(
                              itemCount: ips.length,
                              itemBuilder: (context, index) {
                                return ListTile(
                                  title: Text(ips[index]),
                                  onTap: () {
                                    setState(() {
                                      selectedAddress = ips[index];
                                    });
                                    Navigator.pop(context);
                                  },
                                );
                              },
                            ),
                          ),
                        );
                      }
                    } on PlatformException catch (e) {
                      log("Error: $e");
                      // show a snackbar with the error
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(e.message ??
                                "Unknown error while detecting printers"),
                          ),
                        );
                      }
                    }
                  },
                  child: Text('Detect Printer'),
                ),
                Divider(),
                ElevatedButton(
                  onPressed: () async {
                    final ByteData data =
                        await rootBundle.load('assets/sample.bmp');
                    final Uint8List bytes = data.buffer.asUint8List();
                    await FlutterCitizenPrinter.printImageUSB(bytes);
                  },
                  child: const Text('Print via USB'),
                ),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: () async {
                    final ByteData data =
                        await rootBundle.load('assets/sample.bmp');
                    final Uint8List bytes = data.buffer.asUint8List();
                    await FlutterCitizenPrinter.printImageWiFi(
                        selectedAddress, bytes);
                  },
                  child: const Text('Print via WiFi'),
                ),
              ],
            ),
          );
        }),
      ),
    );
  }
}
