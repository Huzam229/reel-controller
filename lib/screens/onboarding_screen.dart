import 'package:flutter/material.dart';
import 'package:reel_controller/screens/permission_screen.dart';
import 'package:reel_controller/services/app_preferences.dart';
import 'package:reel_controller/theme/app_theme.dart';
import 'package:reel_controller/widgets/primary_button.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _controller = PageController();
  int _page = 0;

  static const _pages = [
    _OnboardPage(
      icon: Icons.back_hand_outlined,
      kicker: 'Reel Controller',
      title: 'Scroll reels without touching the screen',
      body:
          'Leave this app running, open Instagram or TikTok, and let your hand change the video.',
    ),
    _OnboardPage(
      icon: Icons.looks_one_outlined,
      kicker: 'Next reel',
      title: 'Point with your index finger',
      body:
          'Raise only your index finger. The ring finger and pinky stay down. That moves to the next reel.',
    ),
    _OnboardPage(
      icon: Icons.looks_two_outlined,
      kicker: 'Previous reel',
      title: 'Raise your index and middle fingers',
      body:
          'From a fist, lift those two fingers together. A fist by itself only gets ready. It does not scroll.',
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _finish() async {
    await AppPreferences.markOnboardingDone();
    if (!mounted) {
      return;
    }
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const PermissionScreen()),
    );
  }

  void _next() {
    if (_page == _pages.length - 1) {
      _finish();
      return;
    }
    _controller.nextPage(
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    final page = _pages[_page];
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    '${_page + 1} of ${_pages.length}',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: _finish,
                    child: const Text('Skip'),
                  ),
                ],
              ),
              Expanded(
                child: PageView.builder(
                  controller: _controller,
                  itemCount: _pages.length,
                  onPageChanged: (value) => setState(() => _page = value),
                  itemBuilder: (context, index) {
                    final item = _pages[index];
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 28),
                        Container(
                          width: 72,
                          height: 72,
                          decoration: BoxDecoration(
                            color: AppTheme.surfaceHigh,
                            borderRadius: BorderRadius.circular(22),
                          ),
                          child: Icon(item.icon, color: AppTheme.accent, size: 34),
                        ),
                        const SizedBox(height: 28),
                        Text(
                          item.kicker.toUpperCase(),
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: AppTheme.accent,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 1.1,
                              ),
                        ),
                        const SizedBox(height: 12),
                        Text(item.title, style: Theme.of(context).textTheme.headlineMedium),
                        const SizedBox(height: 16),
                        Text(item.body, style: Theme.of(context).textTheme.bodyLarge),
                      ],
                    );
                  },
                ),
              ),
              Row(
                children: List.generate(_pages.length, (index) {
                  final active = index == _page;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    margin: const EdgeInsets.only(right: 8),
                    width: active ? 22 : 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: active ? AppTheme.accent : AppTheme.line,
                      borderRadius: BorderRadius.circular(8),
                    ),
                  );
                }),
              ),
              const SizedBox(height: 22),
              PrimaryButton(
                label: page == _pages.last ? 'Continue' : 'Next',
                onPressed: _next,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _OnboardPage {
  const _OnboardPage({
    required this.icon,
    required this.kicker,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String kicker;
  final String title;
  final String body;
}
