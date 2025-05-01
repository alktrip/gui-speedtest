package org.k3p.speedtest.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class InfoBox extends VBox {
    private final Label iconLabel;
    private final Label titleLabel;
    private final Label valueLabel;

    public InfoBox(String icon, String title, String initialValue) {
        this.setSpacing(5);
        this.setPadding(new Insets(10));
        this.getStyleClass().add("info-box");

        iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("info-box-icon");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-box-title");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("info-box-value");

        this.setAlignment(Pos.CENTER);
        this.getChildren().addAll(iconLabel, titleLabel, valueLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}