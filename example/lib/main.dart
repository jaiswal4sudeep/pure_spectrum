import 'package:flutter/material.dart';
import 'package:pure_spectrum/pure_spectrum.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Center(
          child: FilledButton(
            onPressed: () async {
              await PureSpectrum.showSurvey(token: 'token', userId: 'userId');
            },
            child: Text('Show Survey'),
          ),
        ),
      ),
    );
  }
}
