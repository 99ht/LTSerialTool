package indi.lt.serialtool.view;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AsciiTablePane extends VBox {

    public static class AsciiEntry {
        private final SimpleIntegerProperty code;
        private final SimpleStringProperty character;
        private final SimpleStringProperty hex;
        private final SimpleStringProperty oct;
        private final SimpleStringProperty binary;
        private final SimpleStringProperty description;

        public AsciiEntry(int code, String character, String description) {
            this.code = new SimpleIntegerProperty(code);
            this.character = new SimpleStringProperty(character);
            this.hex = new SimpleStringProperty(String.format("0x%02X", code));
            this.oct = new SimpleStringProperty(String.format("0%03o", code));
            this.binary = new SimpleStringProperty(String.format("%8s", Integer.toBinaryString(code)).replace(' ', '0'));
            this.description = new SimpleStringProperty(description);
        }

        public int getCode() { return code.get(); }
        public String getCharacter() { return character.get(); }
        public String getHex() { return hex.get(); }
        public String getOct() { return oct.get(); }
        public String getBinary() { return binary.get(); }
        public String getDescription() { return description.get(); }
    }

    private final TableView<AsciiEntry> table;
    private final TextField searchField;
    private final Button clearBtn;

    public AsciiTablePane() {
        setSpacing(8);
        setPadding(new Insets(10));

        ObservableList<AsciiEntry> fullList = buildAsciiList();
        FilteredList<AsciiEntry> filtered = new FilteredList<>(fullList, p -> true);

        // --- 表格 ---
        table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AsciiEntry, Integer> colDec = new TableColumn<>("十进制");
        colDec.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDec.setMaxWidth(80);

        TableColumn<AsciiEntry, String> colChar = new TableColumn<>("字符");
        colChar.setCellValueFactory(new PropertyValueFactory<>("character"));
        colChar.setMinWidth(80);

        TableColumn<AsciiEntry, String> colHex = new TableColumn<>("十六进制");
        colHex.setCellValueFactory(new PropertyValueFactory<>("hex"));
        colHex.setMaxWidth(90);

        TableColumn<AsciiEntry, String> colOct = new TableColumn<>("八进制");
        colOct.setCellValueFactory(new PropertyValueFactory<>("oct"));
        colOct.setMaxWidth(90);

        TableColumn<AsciiEntry, String> colBin = new TableColumn<>("二进制");
        colBin.setCellValueFactory(new PropertyValueFactory<>("binary"));
        colBin.setMinWidth(120);

        TableColumn<AsciiEntry, String> colDesc = new TableColumn<>("备注");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setMinWidth(150);

        table.getColumns().addAll(colDec, colChar, colHex, colOct, colBin, colDesc);


        // --- 搜索控件 ---
        searchField = new TextField();
        searchField.setPromptText("输入字符或编码 (如 A, 65, 0x41, 41h)");
        clearBtn = new Button("清除");
        HBox controls = new HBox(8, searchField, clearBtn);

        getChildren().addAll(controls, table);

        // --- 搜索逻辑 ---
        Runnable doFilter = () -> {
            String raw = searchField.getText();
            if (raw == null || raw.trim().isEmpty()) {
                filtered.setPredicate(p -> true);
                return;
            }
            String s = raw.trim();

            try {
                if (s.length() == 1) {
                    char c = s.charAt(0);
                    filtered.setPredicate(entry -> entry.getCode() == c);
                    return;
                }
                if (s.matches("^0x[0-9a-fA-F]+$")) {
                    int val = Integer.parseInt(s.substring(2), 16);
                    filtered.setPredicate(entry -> entry.getCode() == val);
                    return;
                }
                if (s.matches("^[0-9]+$")) {
                    int val = Integer.parseInt(s);
                    filtered.setPredicate(entry -> entry.getCode() == val);
                    return;
                }
                if (s.matches("^[0-9a-fA-F]+[hH]$")) {
                    int val = Integer.parseInt(s.substring(0, s.length() - 1), 16);
                    filtered.setPredicate(entry -> entry.getCode() == val);
                    return;
                }

                String low = s.toLowerCase();
                filtered.setPredicate(entry ->
                        entry.getCharacter().toLowerCase().contains(low) ||
                                entry.getHex().toLowerCase().contains(low) ||
                                entry.getOct().toLowerCase().contains(low) ||
                                entry.getDescription().toLowerCase().contains(low) ||
                                String.valueOf(entry.getCode()).contains(low)
                );

            } catch (NumberFormatException ex) {
                String low = s.toLowerCase();
                filtered.setPredicate(entry ->
                        entry.getCharacter().toLowerCase().contains(low) ||
                                entry.getHex().toLowerCase().contains(low) ||
                                entry.getOct().toLowerCase().contains(low) ||
                                entry.getDescription().toLowerCase().contains(low) ||
                                String.valueOf(entry.getCode()).contains(low)
                );
            }
        };

        searchField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) doFilter.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> doFilter.run());
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filtered.setPredicate(p -> true);
        });
    }

    private ObservableList<AsciiEntry> buildAsciiList() {
        ObservableList<AsciiEntry> list = FXCollections.observableArrayList();

        // 控制字符说明
        String[][] controlChars = {
                {"NUL","Null"},{"SOH","Start of Header"},{"STX","Start of Text"},{"ETX","End of Text"},
                {"EOT","End of Transmission"},{"ENQ","Enquiry"},{"ACK","Acknowledge"},{"BEL","Bell"},
                {"BS","Backspace"},{"TAB","Horizontal Tab"},{"LF","Line Feed"},{"VT","Vertical Tab"},
                {"FF","Form Feed"},{"CR","Carriage Return"},{"SO","Shift Out"},{"SI","Shift In"},
                {"DLE","Data Link Escape"},{"DC1","Device Control 1"},{"DC2","Device Control 2"},{"DC3","Device Control 3"},
                {"DC4","Device Control 4"},{"NAK","Negative Ack"},{"SYN","Synchronous Idle"},{"ETB","End of Block"},
                {"CAN","Cancel"},{"EM","End of Medium"},{"SUB","Substitute"},{"ESC","Escape"},
                {"FS","File Separator"},{"GS","Group Separator"},{"RS","Record Separator"},{"US","Unit Separator"},
                {"DEL","Delete"}
        };

        // 0–31 控制符
        for (int i = 0; i < 32; i++) {
            list.add(new AsciiEntry(i, controlChars[i][0], controlChars[i][1]));
        }

        // 32 空格
        list.add(new AsciiEntry(32, "SP", "Space (空格)"));

        // 33–126 可打印字符
        for (int i = 33; i < 127; i++) {
            char c = (char) i;
            String remark;
            if (Character.isUpperCase(c)) {
                remark = "大写字母 " + c;
            } else if (Character.isLowerCase(c)) {
                remark = "小写字母 " + c;
            } else if (Character.isDigit(c)) {
                remark = "数字 " + c;
            } else {
                // 标点符号描述
                remark = switch (c) {
                    case '!' -> "感叹号";
                    case '"' -> "双引号";
                    case '#' -> "井号 / 井字符";
                    case '$' -> "美元符号";
                    case '%' -> "百分号";
                    case '&' -> "和号 (Ampersand)";
                    case '\'' -> "单引号";
                    case '(' -> "左括号";
                    case ')' -> "右括号";
                    case '*' -> "星号";
                    case '+' -> "加号";
                    case ',' -> "逗号";
                    case '-' -> "减号 / 连字符";
                    case '.' -> "句号 / 点";
                    case '/' -> "斜杠";
                    case ':' -> "冒号";
                    case ';' -> "分号";
                    case '<' -> "小于号";
                    case '=' -> "等号";
                    case '>' -> "大于号";
                    case '?' -> "问号";
                    case '@' -> "艾特符号";
                    case '[' -> "左方括号";
                    case '\\' -> "反斜杠";
                    case ']' -> "右方括号";
                    case '^' -> "脱字符 (Caret)";
                    case '_' -> "下划线";
                    case '`' -> "反引号";
                    case '{' -> "左花括号";
                    case '|' -> "竖线";
                    case '}' -> "右花括号";
                    case '~' -> "波浪号";
                    default -> "符号 " + c;
                };
            }
            list.add(new AsciiEntry(i, Character.toString(c), remark));
        }

        // 127 DEL
        list.add(new AsciiEntry(127, controlChars[32][0], controlChars[32][1]));

        return list;
    }


    public TableView<AsciiEntry> getTableView() {
        return table;
    }
}
