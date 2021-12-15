package com.security.notifypcaccess.alert;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

public class TelegramBotNotify {
	
	// t.me/pcmonitorandnotifybot
	public static String TOKEN = "5087192702:AAF_-UNMrN0xs-aHFzwqwIZPZisyY073bWo";
	
	private TelegramBot bot;
	
	public TelegramBotNotify() {
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			  try {
				  if (bot != null) {
				  	bot.removeGetUpdatesListener();
				  	bot.shutdown();
				  }
			  } catch (Exception e) { // This exception is fine.
			    throw new RuntimeException(e);
			  }
			}));
		
		bot = new TelegramBot(TOKEN);
		
		bot.setUpdatesListener(updates -> {
			
			Update update = updates.get(0);
			CallbackQuery callBack = update.callbackQuery();
			
			Long chatId = 0L;
			String message = "";
			if (callBack != null) {
				chatId = callBack.message().chat().id();
				message = callBack.data();
			}
			else {
				message = update.message().text();
				chatId = update.message().chat().id();
			}
			
		    
			try {
				// ... process updates
				
				return UpdatesListener.CONFIRMED_UPDATES_ALL;
			}
			catch (Exception e) { // This exception is fine. It is in a listener. Can't throw to the Constructor.
				return UpdatesListener.CONFIRMED_UPDATES_ALL;
			}
		});
	}
	
	public void alertDocument(byte[] documentByteArray, String filename, String extention) {
		sendDocument(documentByteArray, filename, extention, 2141146468L);
	}
	
	public void alertMessage(String message) {
		sendMessage(message, 2141146468L);
	}
	
	
	private SendResponse sendMessage(String message, Long chatId) {
		SendResponse response = bot.execute(new SendMessage(chatId, message));
		return response;
	}
	
	/**
	 * 
	 * @param documentByteArray
	 * @param filename
	 * @param extention should include the dot. Ex: .csv
	 * @param chatId
	 * @return
	 */
	private SendResponse sendDocument(byte[] documentByteArray, String filename, String extention, Long chatId) {
		
		SendDocument msg = new SendDocument(chatId, documentByteArray);
		msg.fileName(filename + extention);
		
		SendResponse response = bot.execute(msg);
		return response;
	}

}
