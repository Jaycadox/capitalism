package xyz.jayphen.capitalism.lang;

import net.kyori.adventure.text.Component;

public class Token {
	String content = "";
	TokenType token = null;
	Component component = null;
	String extraData = "";
	
	public Token(String cnt, TokenType tok) {
		this.content = cnt;
		this.token = tok;
	}
	
	public Component getComponent() {
		return component;
	}
	
	public void setComponent(Component component) {
		this.component = component;
	}
	
	public String getExtraData() {
		return extraData;
	}
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	public enum TokenType {
		CAPTION, VARIABLE, BRACKET, COMMAND, CHAT, COMPONENT
	}
}
