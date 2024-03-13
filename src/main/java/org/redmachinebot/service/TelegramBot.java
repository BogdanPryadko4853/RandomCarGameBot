package org.redmachinebot.service;

import lombok.extern.slf4j.Slf4j;
import org.redmachinebot.config.BotConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final ResourceLoader resourceLoader;

    public TelegramBot(BotConfig config, ResourceLoader resourceLoader) {
        this.config = config;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    class PhotoInfo {
        String url;
        String caption;

        public PhotoInfo(String url, String caption) {
            this.url = url;
            this.caption = caption;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                String name = update.getMessage().getFrom().getFirstName();
                startCommandReceived(chatId, name);
            } else if (messageText.equals("Next Car")) {
                PhotoInfo photoInfo = getRandomPhotoInfo();
                sendPhoto(chatId, photoInfo.url, photoInfo.caption);
            }
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update.getCallbackQuery());
        }
    }




    private void processCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        if (callbackQuery.getData().equals("show_random_photo")) {
            PhotoInfo photoInfo = getRandomPhotoInfo();
            sendPhoto(chatId, photoInfo.url, photoInfo.caption);
        }
    }

    private PhotoInfo getRandomPhotoInfo() {
        PhotoInfo[] photoInfos = {
                new PhotoInfo("blue.jpg", "\uD83E\uDEF3\uD83C\uDFFC подать руку"),
                new PhotoInfo("red.jpg", "\uD83D\uDC8B"),
                new PhotoInfo("white.jpg", "ударить по ж***"),
                new PhotoInfo("org.jpg", "✋5\uFE0F⃣"),
                new PhotoInfo("yellow.jpg", "\uD83E\uDD4A\uD83D\uDC4A"),
                new PhotoInfo("purpl.jpg", "ударить кулаком"),
                new PhotoInfo("black.jpg", "укусить"),
                new PhotoInfo("green.jpg", "\uD83E\uDD17")
        };

        double[] probabilities = {0.2, 0.08, 0.15, 0.1, 0.15, 0.1, 0.1, 0.15};

        double sum = 0;
        for (double prob : probabilities) {
            sum += prob;
        }

        double randomNum = Math.random() * sum;

        double cumulativeProb = 0;
        int index = 0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulativeProb += probabilities[i];
            if (randomNum <= cumulativeProb) {
                index = i;
                break;
            }
        }

        return photoInfos[index];
    }

    private ReplyKeyboardMarkup createRandomPhotoButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setResizeKeyboard(true);

        ArrayList<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();

        row1.add(new KeyboardButton("Next Car"));

        keyboard.add(row1);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private void sendMessage(long chatId, String text, ReplyKeyboardMarkup replyMarkup) {

        SendMessage message = new SendMessage();

        message.setChatId(String.valueOf(chatId));

        message.setText(text);

        message.setReplyMarkup(replyMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendPhoto(long chatId, String photoUrl, String caption) {
        try {
            Resource photoResource = resourceLoader.getResource("classpath:/static/" + photoUrl);

            SendPhoto message = new SendPhoto();

            message.setChatId(String.valueOf(chatId));

            message.setPhoto(new InputFile(photoResource.getInputStream(), photoUrl));

            message.setCaption(caption);

            execute(message);

        } catch (TelegramApiException | IOException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }




    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + "\nНажми на кнопку ниже, чтобы посмотреть машины:";
        log.info("Replied to user " + name);
        ReplyKeyboardMarkup keyboard = createRandomPhotoButton();
        sendMessage(chatId, answer, keyboard);
    }
}
