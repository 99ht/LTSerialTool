package indi.lt.serialtool.component;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineCssRegexHighlighter {

    private final PromptInlineCssTextArea area;

    // 外部可绑定的属性：正则、颜色、大小写、透明度
    private final StringProperty patternText = new SimpleStringProperty("");
    private final ObjectProperty<Color> highlightColor = new SimpleObjectProperty<>(Color.web("#fff176"));
    private final BooleanProperty caseInsensitive = new SimpleBooleanProperty(true);
    private final DoubleProperty alpha = new SimpleDoubleProperty(0.45); // 背景透明度 0~1

    // 防抖，避免频繁重绘
    private final PauseTransition debounce = new PauseTransition(Duration.millis(120));

    public InlineCssRegexHighlighter(PromptInlineCssTextArea area) {
        this.area = area;

        // 两个关键参数变化 → 重新高亮
        patternText.addListener((o, a, b) -> schedule());
        highlightColor.addListener((o, a, b) -> schedule());
        caseInsensitive.addListener((o, a, b) -> schedule());
        alpha.addListener((o, a, b) -> schedule());

        // 文本变化也要刷新（例如串口日志追加）
        area.textProperty().addListener((o, a, b) -> schedule());

        debounce.setOnFinished(e -> applyNow());
    }

    /** 供外部在追加/裁剪后手动触发（可选） */
    public void schedule() {
        debounce.playFromStart();
    }

    /** 立刻应用（确保在 FX 线程） */
    public void applyNow() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::applyNow);
            return;
        }

        final String text = area.getText();
        if (text.isEmpty()) {
            area.setStyleSpans(0, plain(text.length()));
            return;
        }

        final String pat = patternText.get();
        if (pat == null || pat.isEmpty()) {
            area.setStyleSpans(0, plain(text.length()));
            return;
        }

        Pattern compiled;
        try {
            int flags = caseInsensitive.get() ? (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : 0;
            compiled = Pattern.compile(pat, flags); // pat 为正则
        } catch (Exception ex) {
            // 非法正则：清空高亮，或你也可以在 UI 显示错误
            area.setStyleSpans(0, plain(text.length()));
            return;
        }

        // 找所有匹配区间
        List<int[]> ranges = new ArrayList<>();
        Matcher m = compiled.matcher(text);
        while (m.find()) {
            if (m.start() == m.end()) continue; // 避免零宽循环
            ranges.add(new int[]{m.start(), m.end()});
        }
        if (ranges.isEmpty()) {
            area.setStyleSpans(0, plain(text.length()));
            return;
        }

        // 合并重叠
        ranges.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        int[] cur = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            int[] n = ranges.get(i);
            if (n[0] <= cur[1]) cur[1] = Math.max(cur[1], n[1]);
            else { merged.add(cur); cur = n; }
        }
        merged.add(cur);

        // 颜色 → 行内 CSS
        String bg = rgba(highlightColor.get(), clamp01(alpha.get()));
        StyleSpans<String> spans = toSpans(text.length(), merged,
                "-rtfx-background-color: " + bg + "; -fx-font-weight: bold;");

        area.setStyleSpans(0, spans);
    }

    private StyleSpans<String> plain(int length) {
        StyleSpansBuilder<String> b = new StyleSpansBuilder<>();
        b.add("", length);
        return b.create();
    }

    private StyleSpans<String> toSpans(int textLen, List<int[]> ranges, String css) {
        StyleSpansBuilder<String> b = new StyleSpansBuilder<>();
        int last = 0;
        for (int[] r : ranges) {
            if (r[0] > last) b.add("", r[0] - last);
            b.add(css, r[1] - r[0]);
            last = r[1];
        }
        if (last < textLen) b.add("", textLen - last);
        return b.create();
    }

    private static String rgba(Color c, double a) {
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    // -------- 属性暴露（可与 TextField / ColorPicker 绑定） --------
    public StringProperty patternTextProperty() { return patternText; }
    public ObjectProperty<Color> highlightColorProperty() { return highlightColor; }
    public BooleanProperty caseInsensitiveProperty() { return caseInsensitive; }
    public DoubleProperty alphaProperty() { return alpha; }

    public void setPatternText(String s) { patternText.set(s); }
    public void setHighlightColor(Color c) { highlightColor.set(c); }
    public void setCaseInsensitive(boolean v) { caseInsensitive.set(v); }
    public void setAlpha(double v) { alpha.set(v); }
}
