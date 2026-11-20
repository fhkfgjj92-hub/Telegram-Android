#!/usr/bin/env python3
"""Apply Telegram mod hooks before Gradle build."""
from pathlib import Path
import sys

ROOT = Path('.')
for candidate in [Path('.'), Path('..')]:
    if (candidate / 'TMessagesProj').is_dir():
        ROOT = candidate.resolve()
        break

def patch(path: Path, old: str, new: str, label: str) -> bool:
    if not path.exists():
        print(f'[skip] missing {path}')
        return False
    text = path.read_text(encoding='utf-8', errors='replace')
    if old not in text:
        if 'Customization.ghostMode()' in text or 'CustomizationActivity' in text:
            print(f'[ok] already patched: {label}')
            return True
        print(f'[warn] pattern not found: {label}')
        return False
    path.write_text(text.replace(old, new, 1), encoding='utf-8')
    print(f'[ok] patched: {label}')
    return True

mc = ROOT / 'TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java'
sa = ROOT / 'TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java'

print('ROOT=', ROOT)

patch(mc,
    "    public void markDialogAsRead(long dialogId, int maxPositiveId, int maxNegativeId, int maxDate, boolean popup, long threadId, int countDiff, boolean readNow, int scheduledCount) {\n        boolean createReadTask;",
    "    public void markDialogAsRead(long dialogId, int maxPositiveId, int maxNegativeId, int maxDate, boolean popup, long threadId, int countDiff, boolean readNow, int scheduledCount) {\n        if (org.telegram.messenger.Customization.ghostMode()) return;\n        boolean createReadTask;",
    "ghostMode markDialogAsRead")

patch(mc,
    "    public boolean sendTyping(long dialogId, long threadMsgId, int action, String emojicon, int classGuid) {\n        if (action < 0 || action >= sendingTypings.length || dialogId == 0) {\n            return false;\n        }",
    "    public boolean sendTyping(long dialogId, long threadMsgId, int action, String emojicon, int classGuid) {\n        if (org.telegram.messenger.Customization.ghostTyping()) return false;\n        if (action < 0 || action >= sendingTypings.length || dialogId == 0) {\n            return false;\n        }",
    "ghostTyping sendTyping")

patch(mc,
    "        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;\n        final boolean quickReplies = mode == ChatActivity.MODE_QUICK_REPLIES;\n        final boolean welcomeMessages = mode == ChatActivity.MODE_WELCOME_MESSAGES;\n        if ((messages == null || messages.isEmpty()) && taskId == 0) {\n            return;\n        }",
    "        final boolean scheduled = mode == ChatActivity.MODE_SCHEDULED;\n        final boolean quickReplies = mode == ChatActivity.MODE_QUICK_REPLIES;\n        final boolean welcomeMessages = mode == ChatActivity.MODE_WELCOME_MESSAGES;\n        try { org.telegram.messenger.DeletedMessagesStore.onDelete(currentAccount, dialogId, messages); } catch (Throwable ignore) {}\n        if ((messages == null || messages.isEmpty()) && taskId == 0) {\n            return;\n        }",
    "saveDeleted deleteMessages")

patch(mc,
    "                        TL_account.updateStatus req = new TL_account.updateStatus();\n                        req.offline = false;\n                        statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {\n                            if (error == null) {\n                                lastStatusUpdateTime = System.currentTimeMillis();\n                                offlineSent = false;\n                                statusSettingState = 0;\n                            } else {\n                                if (lastStatusUpdateTime != 0) {\n                                    lastStatusUpdateTime += 5000;\n                                }\n                            }\n                            statusRequest = 0;\n                        });",
    "                        if (!org.telegram.messenger.Customization.freezeOnline()) {\n                        TL_account.updateStatus req = new TL_account.updateStatus();\n                        req.offline = false;\n                        statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {\n                            if (error == null) {\n                                lastStatusUpdateTime = System.currentTimeMillis();\n                                offlineSent = false;\n                                statusSettingState = 0;\n                            } else {\n                                if (lastStatusUpdateTime != 0) {\n                                    lastStatusUpdateTime += 5000;\n                                }\n                            }\n                            statusRequest = 0;\n                        });\n                        }",
    "freezeOnline online")

patch(mc,
    "                TL_account.updateStatus req = new TL_account.updateStatus();\n                req.offline = true;\n                statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {\n                    if (error == null) {\n                        offlineSent = true;\n                    } else {\n                        if (lastStatusUpdateTime != 0) {\n                            lastStatusUpdateTime += 5000;\n                        }\n                    }\n                    statusRequest = 0;\n                });",
    "                if (!org.telegram.messenger.Customization.freezeOnline()) {\n                TL_account.updateStatus req = new TL_account.updateStatus();\n                req.offline = true;\n                statusRequest = getConnectionsManager().sendRequest(req, (response, error) -> {\n                    if (error == null) {\n                        offlineSent = true;\n                    } else {\n                        if (lastStatusUpdateTime != 0) {\n                            lastStatusUpdateTime += 5000;\n                        }\n                    }\n                    statusRequest = 0;\n                });\n                }",
    "freezeOnline offline")

patch(sa,
    "        items.add(UItem.asHeader(getString(R.string.SettingsHelp)));\n        items.add(SettingCell.Factory.of(17, IconBackgroundColors.ORANGE.top, IconBackgroundColors.ORANGE.bottom, R.drawable.settings_ask, getString(R.string.AskAQuestion)));",
    "        items.add(UItem.asHeader(getString(R.string.SettingsHelp)));\n        items.add(SettingCell.Factory.of(50, IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom, R.drawable.settings_features, LocaleController.getString(R.string.app_mod_settings)));\n        items.add(SettingCell.Factory.of(17, IconBackgroundColors.ORANGE.top, IconBackgroundColors.ORANGE.bottom, R.drawable.settings_ask, getString(R.string.AskAQuestion)));",
    "settings menu item")

patch(sa,
    "            case 17:\n                showDialog(AlertsCreator.createSupportAlert(this, resourceProvider));\n                break;",
    "            case 50:\n                presentFragment(new org.telegram.ui.CustomizationActivity());\n                break;\n            case 17:\n                showDialog(AlertsCreator.createSupportAlert(this, resourceProvider));\n                break;",
    "settings case 50")

if sa.exists():
    st = sa.read_text(encoding='utf-8', errors='replace')
    if 'import org.telegram.messenger.LocaleController;' not in st:
        st2 = st.replace('import org.telegram.messenger.MessagesController;', 'import org.telegram.messenger.LocaleController;\nimport org.telegram.messenger.MessagesController;', 1)
        if st2 != st:
            sa.write_text(st2, encoding='utf-8')
            print('[ok] LocaleController import')

print('patch script finished')
