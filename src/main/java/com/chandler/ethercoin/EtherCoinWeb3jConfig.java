package com.chandler.ethercoin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.http.HttpService;


@Configuration
public class EtherCoinWeb3jConfig  {

	@Value("${cucoin.client.connect}")
	private String connect;
	
	@Value("${cucoin.admin.client}")
	private String adminAllow;
	
	@Bean
	public Web3j web3j() {
		
		return Web3j.build(new HttpService(connect));
	}
	
	@Bean
	public Admin admin() {
		if (("true".equals(adminAllow))) {
			return Admin.build(new HttpService(connect));
		} else {
			return null;
		}
	}

}
