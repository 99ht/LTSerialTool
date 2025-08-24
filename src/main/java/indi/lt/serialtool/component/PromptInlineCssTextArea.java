package indi.lt.serialtool.component;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
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
    private final int maxLines = 50;

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
                        () -> false,
                        area.textProperty(), area.focusedProperty()
                )
        );

        area.setWrapText(true);
        area.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (null == newValue || newValue.isEmpty()) {
                    area.setParagraphGraphicFactory(null);
                } else {
                    area.setParagraphGraphicFactory(LineNumberFactory.get(area));
                }

            }
        });


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

        // 监听滚轮/触控板滚动：只要用户滚动，就停用自动滚动（粘性关闭）
        // 放在这里安装一次即可（本方法只会成功执行一次）
        area.addEventFilter(ScrollEvent.SCROLL, e -> setAutoScroll(false));
        vsPane.addEventFilter(ScrollEvent.SCROLL, e -> setAutoScroll(false));

        Set<Node> bars = vsPane.lookupAll(".scroll-bar");
        for (Node n : bars) {
            if (n instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                verticalBar = sb;

                // 用户用鼠标点击/拖动滚动条（含拖拽滑块、点击轨道）时，停用自动滚动
                verticalBar.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> setAutoScroll(false));
                verticalBar.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> setAutoScroll(false));
                verticalBar.addEventFilter(ScrollEvent.SCROLL, e -> setAutoScroll(false)); // 在滚动条上滚轮

                // 可选：键盘操作滚动条也视为手动干预
                // verticalBar.addEventFilter(KeyEvent.ANY, e -> setAutoScroll(false));

                break;
            }
        }
    }


    private static boolean isAtBottom(ScrollBar sb) {
        return sb.getValue() >= sb.getMax() - EPS;
    }

    // ---------- 对外暴露/转发属性 ----------
    public final StringProperty promptTextProperty() {
        return promptText;
    }

    public final String getPromptText() {
        return promptText.get();
    }

    public final void setPromptText(String value) {
        promptText.set(value);
    }

    public final BooleanProperty editableProperty() {
        return area.editableProperty();
    }

    public final boolean isEditable() {
        return area.isEditable();
    }

    public final void setEditable(boolean value) {
        area.setEditable(value);
    }

    public final BooleanProperty wrapTextProperty() {
        return area.wrapTextProperty();
    }

    public final boolean isWrapText() {
        return area.isWrapText();
    }

    public final void setWrapText(boolean value) {
        area.setWrapText(value);
    }

    public final String getText() {
        return area.getText();
    }

    public final void setText(String value) {
        area.replaceText(value == null ? "" : value);
        if (verticalBar != null) autoScroll.set(isAtBottom(verticalBar));
    }

    public InlineCssTextArea getArea() {
        return area;
    }

    public void setStyleSpans(int i, StyleSpans<String> plain) {
        area.setStyleSpans(i, plain);
    }

    public void appendText(String batch) {
        if (batch == null || batch.isEmpty()) return;
        Runnable r = () -> {
            area.appendText(batch);
            trimByMaxLines(this, maxLines);
            if (autoScroll.get()) moveToEnd();
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /**
     * 使用 RichTextFX 段落 API 按“最大行数”裁剪头部文本。
     */
    private static void trimByMaxLines(PromptInlineCssTextArea area, int maxLines) {
        int paraCount = area.getParagraphs().size();
        if (paraCount <= maxLines) return;

        int remove = paraCount - maxLines;
        // 计算“第 remove 段开头”的全局偏移
        int cutOffset = area.getAbsolutePosition(remove, 0);
        area.replaceText(0, cutOffset, "");
    }

    public void replaceText(int i, int cutOffset, String s) {
        area.replaceText(i, cutOffset, s);
    }

    public int getAbsolutePosition(int remove, int i) {
        return area.getAbsolutePosition(remove, i);
    }

    public LiveList<?> getParagraphs() {
        return area.getParagraphs();
    }

    public ObservableValue<String> textProperty() {
        return area.textProperty();
    }

    public void moveToEnd() {
        area.moveTo(area.getLength());
        area.requestFollowCaret();
    }

    public boolean isAutoScroll() {
        return autoScroll.get();
    }

    public SimpleBooleanProperty autoScrollProperty() {
        return autoScroll;
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll.set(autoScroll);
    }
}
