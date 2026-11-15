package com.modhost.api;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * "Mods" button + panel drawn over the game. List page: toggle mods, see why one can't run, open the
 * gear editor. Editor page: status, settings, config files (text editor).
 */
final class MenuUi {
    private static final String GEAR = "\u2699";
    private static final String[] TEXT_EXT = { ".json", ".json5", ".properties", ".toml", ".txt", ".cfg", ".conf", ".yml", ".yaml" };

    private static Activity host;
    private static Button button;
    private static LinearLayout panel, list;
    private static TextView logView;

    private MenuUi() {}

    static void attach(final Activity a) {
        a.runOnUiThread(new Runnable() {
            public void run() {
                if (host == a && button != null && button.getParent() != null) return;
                remove();
                host = a;

                button = new Button(a);
                button.setText("Mods");
                button.setTextSize(12);
                button.setAllCaps(false);
                button.setTextColor(Color.WHITE);
                button.setBackgroundColor(0xAA000000);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { toggle(a); }
                });
                FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.END);
                bp.setMargins(0, dp(a, 48), dp(a, 8), 0);
                a.addContentView(button, bp);

                panel = buildPanel(a);
                panel.setVisibility(View.GONE);
                int maxH = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.9f);
                a.addContentView(panel, new FrameLayout.LayoutParams(
                        dp(a, 360), Math.min(dp(a, 440), maxH), Gravity.CENTER));
            }
        });
    }

    static void remove() {
        View[] views = { button, panel };
        for (View v : views) {
            if (v != null && v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
        }
        button = null; panel = null; list = null; logView = null; host = null;
    }

    private static void toggle(Activity a) {
        if (panel == null) return;
        if (panel.getVisibility() == View.VISIBLE) {
            panel.setVisibility(View.GONE);
        } else {
            refresh(a);
            panel.setVisibility(View.VISIBLE);
        }
    }

    private static LinearLayout buildPanel(final Activity a) {
        LinearLayout p = new LinearLayout(a);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(0xF0202020);
        int pad = dp(a, 12);
        p.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(a);
        title.setText("Java mods");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        p.addView(title);

        ScrollView modsScroll = new ScrollView(a);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        modsScroll.addView(list);
        p.addView(modsScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 2f));

        p.addView(label(a, "Log", 0xFFAAAAAA));
        ScrollView logScroll = new ScrollView(a);
        logView = new TextView(a);
        logView.setTextColor(0xFFCCCCCC);
        logView.setTextSize(10);
        logScroll.addView(logView);
        p.addView(logScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = new LinearLayout(a);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = new Button(a);
        copy.setText("Copy report");
        copy.setAllCaps(false);
        copy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { copyReport(a); }
        });
        bottom.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = new Button(a);
        close.setText("Close");
        close.setAllCaps(false);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { panel.setVisibility(View.GONE); }
        });
        bottom.addView(close, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        p.addView(bottom);
        return p;
    }

    // ---------------- list page ----------------

    private static void refresh(final Activity a) {
        if (list == null) return;
        list.removeAllViews();
        List<Bootstrap.Entry> mods = Bootstrap.list();
        if (mods.isEmpty()) list.addView(label(a, "No Java mods loaded", 0xFFAAAAAA));
        for (final Bootstrap.Entry e : mods) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox cb = new CheckBox(a);
            cb.setText(e.name + (e.version.length() > 0 ? "  v" + e.version : ""));
            cb.setTextColor(e.status == Bootstrap.OK ? Color.WHITE : 0xFF888888);
            cb.setChecked(e.enabled && e.status == Bootstrap.OK);
            cb.setEnabled(e.status == Bootstrap.OK);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) {
                    Bootstrap.setEnabled(e.id, on);
                    refreshLog();
                }
            });
            row.addView(cb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button gear = new Button(a);
            gear.setText(GEAR);
            gear.setTextSize(18);
            gear.setAllCaps(false);
            gear.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showEditor(a, e); }
            });
            row.addView(gear, new LinearLayout.LayoutParams(dp(a, 52), ViewGroup.LayoutParams.WRAP_CONTENT));
            list.addView(row);

            if (e.status != Bootstrap.OK) {
                TextView why = label(a, e.reason, statusColor(e));
                why.setTextSize(11);
                why.setPadding(dp(a, 32), 0, 0, dp(a, 4));
                list.addView(why);
            }
        }
        refreshLog();
    }

    // ---------------- gear editor page ----------------

    private static void showEditor(final Activity a, final Bootstrap.Entry e) {
        if (list == null) return;
        list.removeAllViews();
        list.addView(label(a, GEAR + "  " + e.name + (e.version.length() > 0 ? "  v" + e.version : ""), Color.WHITE));

        TextView info = label(a, "id: " + e.id + "   type: " + e.kind, 0xFFAAAAAA);
        info.setTextSize(11);
        list.addView(info);
        TextView status = label(a, statusText(e), statusColor(e));
        status.setTextSize(12);
        list.addView(status);
        String about = KnownMods.note(e.id);
        if (about != null) {
            TextView ab = label(a, "About: " + about, 0xFFAAAAAA);
            ab.setTextSize(11);
            list.addView(ab);
        }
        if (e.status == Bootstrap.INCOMPATIBLE) {
            TextView note = label(a, "This mod was written for Minecraft Java Edition and calls game code that "
                    + "Bedrock does not have. It stays off.", 0xFFAAAAAA);
            note.setTextSize(11);
            list.addView(note);
        }

        if (e.status == Bootstrap.OK) {
            CheckBox on = new CheckBox(a);
            on.setText("Enabled");
            on.setTextColor(Color.WHITE);
            on.setChecked(e.enabled);
            on.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean v) { Bootstrap.setEnabled(e.id, v); refreshLog(); }
            });
            list.addView(on);
        }

        // declared settings
        if (e.ctx != null && !e.ctx.config.options().isEmpty()) {
            list.addView(label(a, "Settings", 0xFFAAAAAA));
            addOptions(a, e.ctx.config);
        }

        // config files the mod (or its Fabric config dir) wrote
        list.addView(label(a, "Config files", 0xFFAAAAAA));
        List<File> files = configFiles(e);
        if (files.isEmpty()) {
            TextView none = label(a, "None yet. Mods create these the first time they save settings.", 0xFF888888);
            none.setTextSize(11);
            list.addView(none);
        }
        for (final File f : files) {
            Button b = new Button(a);
            b.setText(f.getName());
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showFile(a, e, f); }
            });
            list.addView(b);
        }

        if (e.ctx != null) {
            TextView path = label(a, "Folder: " + e.ctx.dataDir, 0xFF888888);
            path.setTextSize(10);
            path.setTextIsSelectable(true);
            list.addView(path);
        }

        Button back = new Button(a);
        back.setText("Back");
        back.setAllCaps(false);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { refresh(a); }
        });
        list.addView(back);
        refreshLog();
    }

    private static void addOptions(final Activity a, final ModConfig cfg) {
        for (final ModConfig.Option o : cfg.options()) {
            if (o.type == ModConfig.TOGGLE) {
                CheckBox cb = new CheckBox(a);
                cb.setText(o.label);
                cb.setTextColor(Color.WHITE);
                cb.setChecked(cfg.getBoolean(o.key));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton b, boolean on) { cfg.set(o.key, String.valueOf(on)); refreshLog(); }
                });
                list.addView(cb);
            } else if (o.type == ModConfig.NUMBER) {
                final TextView val = label(a, o.label + ": " + fmt(cfg.getDouble(o.key)), Color.WHITE);
                list.addView(val);
                SeekBar sb = new SeekBar(a);
                sb.setMax(1000);
                sb.setProgress((int) Math.round((cfg.getDouble(o.key) - o.min) / (o.max - o.min) * 1000));
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar s, int p, boolean user) {
                        val.setText(o.label + ": " + fmt(o.min + (o.max - o.min) * p / 1000.0));
                    }
                    public void onStartTrackingTouch(SeekBar s) {}
                    public void onStopTrackingTouch(SeekBar s) {
                        cfg.set(o.key, String.valueOf(o.min + (o.max - o.min) * s.getProgress() / 1000.0));
                        refreshLog();
                    }
                });
                list.addView(sb);
            } else {
                list.addView(label(a, o.label, Color.WHITE));
                LinearLayout row = new LinearLayout(a);
                row.setOrientation(LinearLayout.HORIZONTAL);
                final EditText et = new EditText(a);
                et.setText(cfg.getString(o.key));
                et.setTextColor(Color.WHITE);
                et.setSingleLine(true);
                row.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                Button set = new Button(a);
                set.setText("Set");
                set.setAllCaps(false);
                set.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { cfg.set(o.key, et.getText().toString()); refreshLog(); }
                });
                row.addView(set);
                list.addView(row);
            }
        }
    }

    private static void showFile(final Activity a, final Bootstrap.Entry e, final File f) {
        if (list == null) return;
        list.removeAllViews();
        list.addView(label(a, f.getName(), Color.WHITE));
        if (f.length() > 65536) {
            list.addView(label(a, "File is larger than 64 KB; edit it with a file manager.", 0xFFFFAA33));
        } else {
            final EditText et = new EditText(a);
            et.setText(readText(f));
            et.setTextColor(Color.WHITE);
            et.setTypeface(Typeface.MONOSPACE);
            et.setTextSize(11);
            et.setGravity(Gravity.TOP | Gravity.START);
            et.setMinLines(8);
            list.addView(et);

            Button save = new Button(a);
            save.setText("Save");
            save.setAllCaps(false);
            save.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    boolean ok = writeText(f, et.getText().toString());
                    new ModLogger("ModHost").info((ok ? "saved " : "could not save ") + f.getName());
                    if (ok) Events.post("config.file.changed", f.getAbsolutePath());
                    refreshLog();
                }
            });
            list.addView(save);
        }
        Button back = new Button(a);
        back.setText("Back");
        back.setAllCaps(false);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showEditor(a, e); }
        });
        list.addView(back);
        refreshLog();
    }

    // ---------------- helpers ----------------

    private static List<File> configFiles(Bootstrap.Entry e) {
        List<File> out = new ArrayList<File>();
        if (e.ctx != null) collect(new File(e.ctx.dataDir), null, out);
        collect(new File(Bootstrap.dataRoot(), "config"), e.id.toLowerCase(Locale.US), out);
        return out;
    }

    private static void collect(File dir, String nameMustContain, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        Arrays.sort(fs);
        for (File f : fs) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase(Locale.US);
            if (nameMustContain != null && !n.contains(nameMustContain)) continue;
            for (String ext : TEXT_EXT) if (n.endsWith(ext)) { out.add(f); break; }
        }
    }

    private static String readText(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            } finally { in.close(); }
        } catch (Throwable t) { return ""; }
    }

    private static boolean writeText(File f, String text) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            try { out.write(text.getBytes("UTF-8")); } finally { out.close(); }
            return true;
        } catch (Throwable t) { return false; }
    }

    private static String buildReport() {
        StringBuilder sb = new StringBuilder("ModHost report\n");
        for (Bootstrap.Entry e : Bootstrap.list()) {
            sb.append(e.id).append("  ").append(e.name).append(' ').append(e.version)
              .append("  [").append(e.kind).append("]  ").append(statusText(e))
              .append(e.enabled ? "  (enabled)" : "  (disabled)").append('\n');
        }
        sb.append("\nLog:\n");
        for (String l : ModLogger.recent(80)) sb.append(l).append('\n');
        return sb.toString();
    }

    private static void copyReport(Activity a) {
        String text = buildReport();
        File f = new File(Bootstrap.dataRoot(), "modhost-report.txt");
        boolean saved = writeText(f, text);
        try {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("ModHost report", text));
            new ModLogger("ModHost").info("report copied to clipboard" + (saved ? " and saved to " + f : ""));
        } catch (Throwable t) {
            new ModLogger("ModHost").warn("clipboard unavailable" + (saved ? "; report saved to " + f : ""));
        }
        refreshLog();
    }

    private static String statusText(Bootstrap.Entry e) {
        if (e.status == Bootstrap.OK) return e.active ? "Running" : "Loaded, off";
        return e.reason;
    }

    private static int statusColor(Bootstrap.Entry e) {
        if (e.status == Bootstrap.OK) return 0xFF77DD77;
        return e.status == Bootstrap.INCOMPATIBLE ? 0xFFFFAA33 : 0xFFFF6666;
    }

    private static void refreshLog() {
        if (logView == null) return;
        StringBuilder sb = new StringBuilder();
        for (String l : ModLogger.recent(14)) sb.append(l).append('\n');
        logView.setText(sb.toString());
    }

    private static TextView label(Activity a, String text, int color) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(color);
        return t;
    }

    private static String fmt(double v) { return String.format(Locale.US, "%.1f", v); }

    private static int dp(Activity a, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics());
    }
}
