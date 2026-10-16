with open('app/src/main/java/com/example/model/IPTVModels.kt', 'r') as f:
    text = f.read()
text = text.replace('val showNewsSection: Boolean = true,', 'val showNewsSection: Boolean = true,\n    val showDevSection: Boolean = true,')
with open('app/src/main/java/com/example/model/IPTVModels.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/data/FirebaseService.kt', 'r') as f:
    text = f.read()
text = text.replace('showNewsSection = json.optBoolean("showNewsSection", true),', 'showNewsSection = json.optBoolean("showNewsSection", true),\n                            showDevSection = json.optBoolean("showDevSection", true),')
text = text.replace('put("showNewsSection", config.showNewsSection)', 'put("showNewsSection", config.showNewsSection)\n                put("showDevSection", config.showDevSection)')
with open('app/src/main/java/com/example/data/FirebaseService.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'r') as f:
    text = f.read()
replacement = '''                    AdminSectionToggleRow(
                        title = "قسم الأخبار (News Section)",
                        isChecked = homeConfig.showNewsSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showNewsSection = it)) }
                    )
                    AdminSectionToggleRow(
                        title = "زر لوحة المطور (Developer Button)",
                        isChecked = homeConfig.showDevSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showDevSection = it)) }
                    )'''
text = text.replace('''                    AdminSectionToggleRow(
                        title = "قسم الأخبار (News Section)",
                        isChecked = homeConfig.showNewsSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showNewsSection = it)) }
                    )''', replacement)
with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'w') as f:
    f.write(text)
