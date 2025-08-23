package indi.lt.serialtool.component;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineCssRegexHighlighter {

    private final PromptInlineCssTextArea area;

    private final StringProperty patternText = new SimpleStringProperty("");
    private final ObjectProperty<Color> highlightColor = new SimpleObjectProperty<>(Color.web("#fff176"));
    private final BooleanProperty caseInsensitive = new SimpleBooleanProperty(true);
    private final DoubleProperty alpha = new SimpleDoubleProperty(0.45);

    private final PauseTransition debounce = new PauseTransition(Duration.millis(120));

    // 记录上一次已处理的文本长度
    private int lastLength = 0;

    // 当前编译好的 Pattern（避免每次重新 compile）
    private Pattern currentPattern;

    public InlineCssRegexHighlighter(PromptInlineCssTextArea area) {
        this.area = area;

        // 正则 / 样式参数变化 → 需要全量刷新
        patternText.addListener((o, a, b) -> scheduleFull());
        highlightColor.addListener((o, a, b) -> scheduleFull());
        caseInsensitive.addListener((o, a, b) -> scheduleFull());
        alpha.addListener((o, a, b) -> scheduleFull());

        // 文本变化时 → 增量处理
        area.textProperty().addListener((o, oldText, newText) -> {
            if (newText.length() > oldText.length()) {
                int from = oldText.length();
                int to = newText.length();
                scheduleDelta(from, to);
            } else {
                // 删除/清空 → 全量刷新
                scheduleFull();
            }
        });

        debounce.setOnFinished(e -> applyNow());
    }

    /**
     * 增量高亮区间
     */
    private int deltaFrom = -1, deltaTo = -1;
    private boolean fullRefresh = false;

    private void scheduleDelta(int from, int to) {
        this.deltaFrom = from;
        this.deltaTo = to;
        this.fullRefresh = false;
        debounce.playFromStart();
    }

    private void scheduleFull() {
        this.fullRefresh = true;
        debounce.playFromStart();
    }

    public void applyNow() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::applyNow);
            return;
        }

        final String text = area.getText();
        if (text.isEmpty()) {
            lastLength = 0;
            return;
        }

        // 编译正则
        final String pat = patternText.get();
        if (pat == null || pat.isEmpty()) {
            lastLength = text.length();
            return;
        }
        try {
            int flags = caseInsensitive.get() ? (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : 0;
            currentPattern = Pattern.compile(pat, flags);
        } catch (Exception ex) {
            currentPattern = null;
            return;
        }

        String css = "-rtfx-background-color: " +
                rgba(highlightColor.get(), clamp01(alpha.get())) +
                "; -fx-font-weight: bold;";

        if (fullRefresh) {
            // 全量刷新
            Matcher m = currentPattern.matcher(text);
            while (m.find()) {
                if (m.start() == m.end()) continue;
                area.getArea().setStyle(m.start(), m.end(), css);
            }
            lastLength = text.length();
        } else if (deltaFrom >= 0 && deltaTo > deltaFrom) {
            // 增量处理新增区间
            String delta = text.substring(deltaFrom, deltaTo);
            Matcher m = currentPattern.matcher(delta);
            while (m.find()) {
                if (m.start() == m.end()) continue;
                int start = deltaFrom + m.start();
                int end = deltaFrom + m.end();
                area.getArea().setStyle(start, end, css);
            }
            lastLength = text.length();
        }

        deltaFrom = deltaTo = -1;
    }

    private static String rgba(Color c, double a) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    // -------- 属性暴露 --------
    public StringProperty patternTextProperty() {
        return patternText;
    }

    public ObjectProperty<Color> highlightColorProperty() {
        return highlightColor;
    }

    public BooleanProperty caseInsensitiveProperty() {
        return caseInsensitive;
    }

    public DoubleProperty alphaProperty() {
        return alpha;
    }

    public void setPatternText(String s) {
        patternText.set(s);
    }

    public void setHighlightColor(Color c) {
        highlightColor.set(c);
    }

    public void setCaseInsensitive(boolean v) {
        caseInsensitive.set(v);
    }

    public void setAlpha(double v) {
        alpha.set(v);
    }

    // ========= 在类里加这三个公共方法 =========

    // 兼容旧调用：触发一次“全量刷新”
    public void schedule() {
        scheduleFull();
    }

    // 显式全量刷新（可在外部调用）
    public void refreshAll() {
        scheduleFull();
    }

    // 如果你在外部知道新增区间，也可以显式只刷这段
    public void highlightNewAppend(int from, int to) {
        scheduleDelta(from, to);
    }

}
