package aotstory.client;

import aotstory.net.DialogueChoicePayload;
import aotstory.net.DialogueNodePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Простое окно диалога: реплика сверху, кнопки ответов снизу.
 * Клиент ничего не решает — он шлёт серверу номер нажатой кнопки и ждёт следующий узел.
 */
public class DialogueScreen extends Screen {
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    private static final int MARGIN_BOTTOM = 20;

    private String speaker;
    private String text;
    private List<String> choices;

    public DialogueScreen(DialogueNodePayload payload) {
        super(Text.literal("Диалог"));
        apply(payload);
    }

    private void apply(DialogueNodePayload payload) {
        this.speaker = payload.speaker();
        this.text = payload.text();
        this.choices = payload.choices();
    }

    public void update(DialogueNodePayload payload) {
        apply(payload);
        this.clearAndInit();
    }

    private int boxWidth() {
        return Math.min(this.width - 40, 360);
    }

    @Override
    protected void init() {
        int w = boxWidth();
        int x = (this.width - w) / 2;
        int top = this.height - MARGIN_BOTTOM - choices.size() * (BUTTON_H + GAP);
        for (int i = 0; i < choices.size(); i++) {
            final int index = i;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(choices.get(i)),
                            button -> ClientPlayNetworking.send(new DialogueChoicePayload(index)))
                    .dimensions(x, top + i * (BUTTON_H + GAP), w, BUTTON_H)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int w = boxWidth();
        int x = (this.width - w) / 2;
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(text), w - 16);
        int boxHeight = 24 + lines.size() * 11 + 6;
        int bottom = this.height - MARGIN_BOTTOM - choices.size() * (BUTTON_H + GAP) - 6;
        int top = bottom - boxHeight;

        context.fill(x, top, x + w, bottom, 0xC0000000);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(speaker).formatted(Formatting.GOLD, Formatting.BOLD), x + 8, top + 6, 0xFFFFFF);
        int y = top + 20;
        for (OrderedText line : lines) {
            context.drawTextWithShadow(this.textRenderer, line, x + 8, y, 0xFFFFFF);
            y += 11;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
