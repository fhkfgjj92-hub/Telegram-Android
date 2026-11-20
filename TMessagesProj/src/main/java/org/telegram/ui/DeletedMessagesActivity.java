package org.telegram.ui;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import org.telegram.messenger.DeletedMessagesStore;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/** Экран просмотра удалённых собеседником сообщений. */
public class DeletedMessagesActivity extends BaseFragment {

    private RecyclerListView listView;
    private TextView emptyView;
    private ArrayList<DeletedMessagesStore.Entry> entries = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.app_mod_deleted));
        actionBar.setAllowOverlayTitle(true);
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.msg_delete);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                    return;
                }
                if (id == 1) {
                    new AlertDialog.Builder(getParentActivity())
                            .setTitle("Очистить")
                            .setMessage("Удалить все сохранённые записи?")
                            .setPositiveButton("Удалить", (d, w) -> {
                                DeletedMessagesStore.clear();
                                reload();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                }
            }
        });

        FrameLayout frame = new FrameLayout(context);
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(new Adapter());
        listView.setOnItemClickListener((view, position, x, y) -> {
            DeletedMessagesStore.Entry e = entries.get(position);
            new AlertDialog.Builder(getParentActivity())
                    .setTitle(e.chat)
                    .setMessage(e.text)
                    .setPositiveButton("Закрыть", null)
                    .show();
        });
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setText("Пока ничего не сохранено.\nЗдесь появятся сообщения, удалённые собеседником.");
        emptyView.setTextSize(15);
        emptyView.setTextColor(0xff808080);
        emptyView.setGravity(Gravity.CENTER);
        frame.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        fragmentView = frame;
        reload();
        return fragmentView;
    }

    private void reload() {
        entries = DeletedMessagesStore.all();
        boolean empty = entries.isEmpty();
        if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (listView != null) {
            listView.setVisibility(empty ? View.GONE : View.VISIBLE);
            listView.getAdapter().notifyDataSetChanged();
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {
        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }

        @Override
        public int getItemCount() { return entries.size(); }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextCell cell = new TextCell(parent.getContext());
            cell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            DeletedMessagesStore.Entry e = entries.get(position);
            TextCell cell = (TextCell) holder.itemView;
            String time = DateFormat.format("dd.MM.yyyy HH:mm", e.ts).toString();
            String snippet = e.text == null ? "" : (e.text.length() > 30 ? e.text.substring(0, 30) + "…" : e.text);
            cell.setTextAndValue(e.chat + " · " + time, snippet, position != entries.size() - 1);
        }
    }
}
