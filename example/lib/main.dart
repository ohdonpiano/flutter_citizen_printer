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
  final addressController = TextEditingController();
  String usbStatus = "";

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Citizen Printer Example')),
        body: Builder(builder: (context) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(usbStatus),
                    ElevatedButton(
                      onPressed: () async {
                        try {
                          final res =
                              await FlutterCitizenPrinter.getUsbStatus();
                          log("USB Status: $res");
                          setState(() {
                            usbStatus = res.map((e) => e.name).join(", ");
                          });
                        } catch (e) {
                          log("USB Status error: $e");
                          if (!mounted) return;
                          setState(() {
                            usbStatus = "Error: $e";
                          });
                        }
                      },
                      child: const Text('Get USB Status'),
                    ),
                    TextFormField(
                      controller: addressController,
                      decoration: InputDecoration(labelText: "IP Address"),
                    ),
                    ElevatedButton(
                      onPressed: () async {
                        try {
                          final ips =
                              await FlutterCitizenPrinter.detectPrinters();
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
                                      title: Text(ips[index].ipAddress),
                                      onTap: () {
                                        setState(() {
                                          addressController.text =
                                              ips[index].ipAddress;
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
                            addressController.text, bytes);
                      },
                      child: const Text('Print via WiFi'),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
