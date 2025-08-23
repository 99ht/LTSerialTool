package indi.lt.serialtool.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.reactfx.collection.LiveList;

public class PromptInlineCssTextArea extends StackPane {

    private final InlineCssTextArea area = new InlineCssTextArea();

    // 在自定义控件上定义一个 JavaFX 属性，供 FXML 使用
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

    public PromptInlineCssTextArea() {   // FXML 需要无参构造
        setPadding(new Insets(2, 2, 2, 2));
        Label promptLabel = new Label();
        getChildren().addAll(area, promptLabel);

        // 提示文字样式/布局
        promptLabel.getStyleClass().add("prompt-text");
        promptLabel.setMouseTransparent(true);               // 不拦截鼠标
        StackPane.setAlignment(promptLabel, Pos.TOP_LEFT);

        // 让 Label 文本跟随 promptText 属性
        promptLabel.textProperty().bind(promptTextProperty());

        // 显示条件：内容为空 且 未聚焦（和 TextArea 的行为一致）
        promptLabel.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> area.getText().isEmpty() && !area.isFocused(),
                        area.textProperty(), area.focusedProperty()
                )
        );

        // 你原来对 area 的初始化（wrapText 等）也可以放这
        area.setWrapText(true);
    }

    /* -------- 转发/暴露属性：让 FXML 能识别这些属性 -------- */

    // promptText（自定义新属性）
    public final StringProperty promptTextProperty() {
        return promptText;
    }

    public final String getPromptText() {
        return promptText.get();
    }

    public final void setPromptText(String value) {
        promptText.set(value);
    }

    // editable（转发给内部的 InlineCssTextArea）
    public final BooleanProperty editableProperty() {
        return area.editableProperty();
    }

    public final boolean isEditable() {
        return area.isEditable();
    }

    public final void setEditable(boolean value) {
        area.setEditable(value);
    }

    // wrapText（也常用）
    public final BooleanProperty wrapTextProperty() {
        return area.wrapTextProperty();
    }

    public final boolean isWrapText() {
        return area.isWrapText();
    }

    public final void setWrapText(boolean value) {
        area.setWrapText(value);
    }

    // 如需在 FXML 里设置初始文本，可提供：
    public final String getText() {
        return area.getText();
    }

    public final void setText(String value) {
        area.replaceText(value == null ? "" : value);
    }

    // 可选：暴露内部区域，便于代码里继续使用
    public InlineCssTextArea getArea() {
        return area;
    }

    public void setStyleSpans(int i, StyleSpans<String> plain) {
        area.setStyleSpans(i, plain);
    }

    public void appendText(String batch) {
        area.appendText(batch);
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
}
