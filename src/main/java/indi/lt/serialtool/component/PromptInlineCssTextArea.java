package indi.lt.serialtool.component;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.reactfx.collection.LiveList;

import java.util.Set;

public class PromptInlineCssTextArea extends StackPane {

    private final InlineCssTextArea area = new InlineCssTextArea();
    private final VirtualizedScrollPane<InlineCssTextArea> vsPane;

    private final SimpleBooleanProperty autoScroll = new SimpleBooleanProperty(true);
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

    // 缓存竖直滚动条
    private ScrollBar verticalBar;
    private static final double EPS = 1e-3;

    public PromptInlineCssTextArea() {
        setPadding(new Insets(2, 2, 2, 2));
        Label promptLabel = new Label();

        vsPane = new VirtualizedScrollPane<>(area);
        getChildren().addAll(vsPane, promptLabel);

        promptLabel.getStyleClass().add("prompt-text");
        promptLabel.setMouseTransparent(true);
        StackPane.setAlignment(promptLabel, Pos.TOP_LEFT);
        promptLabel.textProperty().bind(promptTextProperty());
        promptLabel.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> area.getText().isEmpty() && !area.isFocused(),
                        area.textProperty(), area.focusedProperty()
                )
        );

        area.setWrapText(true);

        // —— 等待进入 Scene 后再尝试安装滚动条监听（无 skinProperty 可用）
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::installScrollBarListenerIfNeeded);
            }
        });
        // 首次布局/大小变化时再尝试一次（防止过早 lookup 为空）
        vsPane.layoutBoundsProperty().addListener((o, ov, nv) -> installScrollBarListenerIfNeeded());
    }

    private void installScrollBarListenerIfNeeded() {
        if (verticalBar != null || getScene() == null) return;

        // 确保已应用 CSS/完成一次布局，再 lookup
        vsPane.applyCss();
        vsPane.layout();

        Set<Node> bars = vsPane.lookupAll(".scroll-bar");
        for (Node n : bars) {
            if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                verticalBar = sb;

                // 用户滚动时：离开底部 -> 关自动滚动；到达底部 -> 开启
                verticalBar.valueProperty().addListener((o, ov, nv) -> autoScroll.set(isAtBottom(verticalBar)));
                // 范围变化时也刷新一次（避免精度/布局抖动）
                verticalBar.maxProperty().addListener((o, ov, nv) -> autoScroll.set(isAtBottom(verticalBar)));
                verticalBar.visibleAmountProperty().addListener((o, ov, nv) -> autoScroll.set(isAtBottom(verticalBar)));

                // 初始化一次状态
                autoScroll.set(isAtBottom(verticalBar));
                break;
            }
        }
    }

    private static boolean isAtBottom(ScrollBar sb) {
        return sb.getValue() >= sb.getMax() - EPS;
    }

    // ---------- 对外暴露/转发属性 ----------
    public final StringProperty promptTextProperty() { return promptText; }
    public final String getPromptText() { return promptText.get(); }
    public final void setPromptText(String value) { promptText.set(value); }

    public final BooleanProperty editableProperty() { return area.editableProperty(); }
    public final boolean isEditable() { return area.isEditable(); }
    public final void setEditable(boolean value) { area.setEditable(value); }

    public final BooleanProperty wrapTextProperty() { return area.wrapTextProperty(); }
    public final boolean isWrapText() { return area.isWrapText(); }
    public final void setWrapText(boolean value) { area.setWrapText(value); }

    public final String getText() { return area.getText(); }
    public final void setText(String value) {
        area.replaceText(value == null ? "" : value);
        if (verticalBar != null) autoScroll.set(isAtBottom(verticalBar));
    }

    public InlineCssTextArea getArea() { return area; }

    public void setStyleSpans(int i, StyleSpans<String> plain) { area.setStyleSpans(i, plain); }

    public void appendText(String batch) {
        if (batch == null || batch.isEmpty()) return;
        Runnable r = () -> {
            area.appendText(batch);
            if (autoScroll.get()) moveToEnd();
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    public void replaceText(int i, int cutOffset, String s) {
        area.replaceText(i, cutOffset, s);
        if (verticalBar != null) autoScroll.set(isAtBottom(verticalBar));
    }

    public int getAbsolutePosition(int remove, int i) { return area.getAbsolutePosition(remove, i); }

    public LiveList<?> getParagraphs() { return area.getParagraphs(); }

    public ObservableValue<String> textProperty() { return area.textProperty(); }

    public void moveToEnd() {
        area.moveTo(area.getLength());
        area.requestFollowCaret();
    }

    public boolean isAutoScroll() { return autoScroll.get(); }
    public SimpleBooleanProperty autoScrollProperty() { return autoScroll; }
    public void setAutoScroll(boolean autoScroll) { this.autoScroll.set(autoScroll); }
}
