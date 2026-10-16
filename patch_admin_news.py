import re

with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'r') as f:
    text = f.read()

replacement = '''                    AdminSectionToggleRow(
                        title = "قسم الأخبار (News Section)",
                        isChecked = homeConfig.showNewsSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showNewsSection = it)) }
                    )
                    AdminSectionToggleRow(
                        title = "عداد المتواجدين الآن (Online Users)",'''

text = text.replace('                    AdminSectionToggleRow(\n                        title = "عداد المتواجدين الآن (Online Users)",', replacement)

with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'w') as f:
    f.write(text)
