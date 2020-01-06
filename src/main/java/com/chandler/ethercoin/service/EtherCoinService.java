package com.chandler.ethercoin.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.NewAccountIdentifier;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import com.chandler.ethercoin.contract.SimpleStorage;

import rx.Observable;

@Service
public class EtherCoinService {
	private static Logger log = LoggerFactory.getLogger(EtherCoinService.class);

	@Autowired
	private Web3j web3j;
	
	@Autowired
	private Admin admin;

	/**
	 * geth client 버전 조회.  ooooo
	 */
	public String getClientVersion() throws InterruptedException, ExecutionException {
		
		Web3ClientVersion client = web3j.web3ClientVersion().sendAsync().get();
		
		String clientVersion = client.getWeb3ClientVersion();
		log.debug("clientVersion={}", clientVersion);
		return clientVersion;
	}
	
	/**
	 * geth client 버전 조회.  ooooo
	 */
    public String getClientVersion2() throws IOException {    	
        Web3ClientVersion client = web3j.web3ClientVersion().send();
        
        String clientVersion = client.getWeb3ClientVersion();
        log.debug("clientVersion={}", clientVersion);
        return clientVersion;
    }

    /**
     * ether base 조회  eth.coinbase
     * @param web3j
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
	public EthCoinbase getCoinbase() throws InterruptedException, ExecutionException {
		return web3j
				.ethCoinbase()
				.sendAsync()
				.get();
	}
	
	/**
	 * 계정 리스트 조회.  eth.accounts
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public List<String> getAccounts() throws InterruptedException, ExecutionException {
		
		EthAccounts accountList =  web3j.ethAccounts().sendAsync().get();
		
		return accountList.getAccounts(); 
	}
	
	/**
	 * 잔고조회  eth.getBalance(eth.accounts[0])
	 * @param eoaAddress
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
    public BigInteger getBalance(String eoaAddress) throws IOException, InterruptedException, ExecutionException {
    	// send asynchronous requests to get balance
    	EthGetBalance ethGetBalance = web3j
    												.ethGetBalance(eoaAddress, DefaultBlockParameterName.LATEST)
    												.sendAsync()
    												.get();

    	BigInteger wei = ethGetBalance.getBalance();
    	
    	log.debug("balance={}", wei);
    	return wei;
    }
    
    /**
     * 아직미검증.............  계정생성
     * @param admin
     * @param passphrase
     * @return
     * @throws IOException
     */
    public String newAccount(String passphrase) throws IOException {

    	if (null == admin) {
    		return "어드민 접속이 허용되어있지 않네요....."; // 나중에 익셉션 throws해야....
    	}
    	
    	NewAccountIdentifier newAccountIdentifier = admin.personalNewAccount(passphrase).send();
		String addressOfEOA = newAccountIdentifier.getAccountId();
		log.debug("New account EOA address: {}", addressOfEOA);
		
		log.info("@~~@~~" + newAccountIdentifier.toString());
		log.info("@~~@~~" + newAccountIdentifier.getJsonrpc());
		log.info("@~~@~~" + newAccountIdentifier.getRawResponse());

		return addressOfEOA;    	
    }
    
    public Boolean unlockAccount(String eoa, String pass) {
    	
    	if (null == admin) {
    		System.out.println("어드민 접속이 허용되어있지 않네요....."); // 나중에 익셉션 throws해야....
    		return false;
    	}
    	
		Boolean isUnlocked = false;
		// 계정 잠금 해제 시간 단위 초 기본값 300 초 :: 여기서는 60초
		BigInteger unlockDuration = BigInteger.valueOf(60L);
		try {

			PersonalUnlockAccount personalUnlockAccount = admin.personalUnlockAccount(eoa, pass, unlockDuration).send();
			isUnlocked = personalUnlockAccount.accountUnlocked();
			log.debug("Account unlock {}", isUnlocked);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return isUnlocked;
	}
    
    /**
     * value전송
     * @param fromCredentials : 송신자 credentials
     * @param senderPpassphrase : 송신자 pass
     * @param toEoa : 수신자 어카운트어드레스
     * @param value : 전송하고자 하는 value
     * @return
     * @throws TransactionException
     * @throws Exception
     */
    public TransactionReceipt valueTransfer(Credentials fromCredentials, String senderPpassphrase, String toEoa, BigDecimal value) throws TransactionException, Exception{
		
        if (fromCredentials == null) {
        	System.out.println("@~~@~~ Credentials IS NULL.............................");
        
        	return null;
        }
        
        PersonalUnlockAccount personalUnlockAccount = admin.personalUnlockAccount(fromCredentials.getAddress(), senderPpassphrase).sendAsync().get();
        TransactionReceipt receipt = null;
        
        System.out.println("송신자의 잔고: " + this.getBalance(fromCredentials.getAddress()));
        System.out.println("수신자의 잔고: " + this.getBalance(toEoa));
        
        if (personalUnlockAccount.accountUnlocked()) {
	    	receipt = Transfer.sendFunds(
	                web3j, fromCredentials,
	                toEoa,
	                value, Convert.Unit.ETHER)  // 1 wei = 10^-18 Ether
	                .sendAsync().get();
	        
	    	admin.ethMining();
        	web3j.ethMining();
        	
	        System.out.println("Transaction complete : " + receipt.getTransactionHash());
	        
	        System.out.println("이체후 송신자의 잔고: " + this.getBalance(fromCredentials.getAddress()));
	        System.out.println("이체후 수신자의 잔고: " + this.getBalance(toEoa));
	        
        } else {
        	System.out.println("계정이 언락 되어있지 않습니다.");
        	receipt = null;
        }

        return receipt;
    }
    
    /**
     * simple storage Smart Contract 제어.....
     */
    public void simpleStorageContract() {

        Credentials credentials = null;
        String passphrase =  "curos01";
        String keyStorePath = "D:\\Ethereum\\keystore\\UTC--2018-06-26T07-30-50.243025700Z--ca244eeb8ee6508213eeefa29ec8938126e1fb2e";
        //----임시시시시ㅂ---------------------------------------------------------------------------------
/**        
        File file = new File(keyStorePath);
        if (file.exists()) {
        	System.out.println("화일이 존재하네요... file사이즈=" + file.length() + "Kbyte");
        } else {
        	System.out.println("화일이 존제 하지 않습니다... ");
        }
*/        
        //-------------------------------------------------------------------------------------
		try {
			credentials = WalletUtils.loadCredentials(passphrase, keyStorePath);
		} catch (IOException | CipherException e) {
			e.printStackTrace();
			System.out.println("@~~@~~@~~" + e.getLocalizedMessage());
		}
		
        if (credentials == null) {
        	System.out.println("@~~@~~ Credentials IS NULL.............................");
        
        	return;
        }

        long GAS_PRICE = 44000;
        long GAS_LIMIT = 2100000;

        System.out.println("지금부터 smart contract 배포~");
        SimpleStorage contract = null;
		
        try {
			contract = SimpleStorage.deploy(web3j, credentials, BigInteger.valueOf(GAS_PRICE), BigInteger.valueOf(GAS_LIMIT)).send();
		
        } catch (Exception e) {
			e.printStackTrace();
			System.out.println("Smart contract 에러.............. " + e.getLocalizedMessage());
		}

        String contractAddress = contract.getContractAddress();
        System.out.println("Smart contract 배포된 어드레스 " + contractAddress);

        System.out.println("Smart contract에 있는 storedData에 값 세팅");
       
    	try {
			contract.set(BigInteger.valueOf(111)).send();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Smart contract 에러.............. " + e.getLocalizedMessage());
		}

        try {
			System.out.println("Smart contract에 최종값 확인 : " + contract.get().send());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Smart contract ERROR.............. " + e.getLocalizedMessage());
		}
    }
    
    public Observable<EthBlock> blockObservable(boolean a) {
    	
    	Observable<EthBlock> obv = web3j.blockObservable(a); 

    	return obv;
    }
}
