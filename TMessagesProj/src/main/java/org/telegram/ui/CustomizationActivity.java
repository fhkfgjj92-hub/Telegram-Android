package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import org.telegram.messenger.Customization;
import org.telegram.messenger.DeletedMessagesStore;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/** Экран настроек мода: «Настройки → Мод: кастомизация». */
public class CustomizationActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private static final int ROW_GHOST     = 1;
    private static final int ROW_TYPING    = 2;
    private static final int ROW_ONLINE    = 3;
    private static final int ROW_SAVE_DEL  = 4;
    private static final int ROW_VIEW_DEL  = 5;
    private static final int ROW_ACCENT    = 7;
    private static final int ROW_EXPORT    = 9;
    private static final int ROW_RESET     = 10;
    private static final int ROW_ABOUT     = 11;

    private static final String[] ACCENT_NAMES = {
            "Стандартный (Telegram)", "Красный", "Зелёный", "Фиолетовый",
            "Оранжевый", "Розовый", "Бирюзовый", "Индиго", "Серый"
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.app_mod_settings));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout frame = new FrameLayout(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position, x, y) -> onRowClick(position));
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = frame;
        return fragmentView;
    }

    private void onRowClick(int position) {
        switch (position) {
            case ROW_GHOST:
                Customization.set("ghost_mode", !Customization.ghostMode());
                break;
            case ROW_TYPING:
                Customization.set("ghost_typing", !Customization.ghostTyping());
                break;
            case ROW_ONLINE:
                Customization.set("freeze_online", !Customization.freezeOnline());
                break;
            case ROW_SAVE_DEL:
                Customization.set("save_deleted", !Customization.saveDeleted());
                break;
            case ROW_VIEW_DEL:
                presentFragment(new DeletedMessagesActivity());
                return;
            case ROW_ACCENT:
                showAccentDialog();
                return;
            case ROW_EXPORT:
                exportSettings();
                return;
            case ROW_RESET:
                new AlertDialog.Builder(getParentActivity())
                        .setTitle("Сброс настроек")
                        .setMessage("Вернуть настройки мода к значениям по умолчанию? Сохранённые удалённые сообщения тоже будут стёрты.")
                        .setPositiveButton("Сбросить", (d, w) -> {
                            Customization.reset();
                            DeletedMessagesStore.clear();
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                return;
            case ROW_ABOUT:
                new AlertDialog.Builder(getParentActivity())
                        .setTitle("Мод для Telegram")
                        .setMessage("Локальный мод на базе открытого исходного кода Telegram.\n\n" +
                                "• Ghost Mode — без отметок «прочитано»\n" +
                                "• Скрытие «печатает…» и заморозка «в сети»\n" +
                                "• Сохранение удалённых сообщений\n" +
                                "• Акцентные цвета интерфейса\n\n" +
                                "Не связан с Telegram FZ-LLC. Используйте на свой страх и риск.")
                        .setPositiveButton("ОК", null)
                        .show();
                return;
        }
        adapter.notifyDataSetChanged();
    }

    private void showAccentDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Акцентный цвет");
        b.setItems(ACCENT_NAMES, (dialog, which) -> {
            Customization.setAccent(which);
            adapter.notifyDataSetChanged();
            if (listView != null) listView.invalidate();
        });
        b.show();
    }

    private void exportSettings() {
        try {
            JSONObject o = new JSONObject();
            o.put("ghost_mode", Customization.ghostMode());
            o.put("ghost_typing", Customization.ghostTyping());
            o.put("freeze_online", Customization.freezeOnline());
            o.put("save_deleted", Customization.saveDeleted());
            o.put("accent", Customization.accentColor());
            o.put("deleted_stored", DeletedMessagesStore.count());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, o.toString(2));
            getParentActivity().startActivity(Intent.createChooser(i, "Экспорт настроек"));
        } catch (Exception e) {
            // ignore
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context ctx;

        ListAdapter(Context context) { ctx = context; }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }

        @Override
        public int getItemCount() { return 12; }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || position == 6 || position == 8) return 0;
            if (position == ROW_ACCENT) return 2;
            if (position == ROW_VIEW_DEL || position == ROW_EXPORT
                    || position == ROW_RESET || position == ROW_ABOUT) return 3;
            return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            switch (viewType) {
                case 0: v = new HeaderCell(ctx); break;
                case 2: v = new TextSettingsCell(ctx); break;
                case 3: v = new TextCell(ctx); break;
                default: v = new TextCheckCell(ctx);
            }
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case 0: {
                    HeaderCell h = (HeaderCell) holder.itemView;
                    if (position == 0) h.setText("Приватность");
                    else if (position == 6) h.setText("Внешний вид");
                    else h.setText("Данные");
                    break;
                }
                case 1: {
                    TextCheckCell c = (TextCheckCell) holder.itemView;
                    if (position == ROW_GHOST) c.setTextAndCheck("Ghost Mode — без отметок «прочитано»", Customization.ghostMode(), true);
                    else if (position == ROW_TYPING) c.setTextAndCheck("Скрывать «печатает…» и «записывает»", Customization.ghostTyping(), true);
                    else if (position == ROW_ONLINE) c.setTextAndCheck("Заморозить статус «в сети»", Customization.freezeOnline(), true);
                    else c.setTextAndCheck("Сохранять удалённые сообщения", Customization.saveDeleted(), false);
                    break;
                }
                case 2: {
                    TextSettingsCell c = (TextSettingsCell) holder.itemView;
                    int cur = Customization.accent();
                    String name = (cur >= 0 && cur < ACCENT_NAMES.length) ? ACCENT_NAMES[cur] : "Стандартный";
                    c.setTextAndValue("Акцентный цвет", name, false);
                    break;
                }
                case 3: {
                    TextCell c = (TextCell) holder.itemView;
                    if (position == ROW_VIEW_DEL) {
                        c.setTextAndValue("Удалённые сообщения", String.valueOf(DeletedMessagesStore.count()), true);
                    } else if (position == ROW_EXPORT) {
                        c.setText("Экспорт настроек", true);
                    } else if (position == ROW_RESET) {
                        c.setText("Сбросить настройки мода", false);
                        c.setTextColor(0xffe53935);
                    } else {
                        c.setText("О моде", false);
                    }
                    break;
                }
            }
        }
    }
}
