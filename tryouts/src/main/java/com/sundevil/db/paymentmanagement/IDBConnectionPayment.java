/**
 * 
 */
package com.sundevil.db.paymentmanagement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.sundevil.db.ADBConnectionManager;

/**
 * @author satyaswaroop
 * 
 */
@Service
public class IDBConnectionPayment extends ADBConnectionManager {

	public boolean createPayment(String paymentId, String accountId,
			String amount) {
		JdbcTemplate insert = new JdbcTemplate(dataSource);
		String updateStatement = "INSERT INTO bank.payments (PaymentId,OriginAccount,Amount) VALUES (?, ?, ?)";
		insert.update(updateStatement, new Object[] { paymentId, accountId,
				amount });
		return true;
	}

	public boolean updatePayment(String paymentId) {
		JdbcTemplate update = new JdbcTemplate(dataSource);
		String updatePaymentStatus = "UPDATE bank.payments SET Status=1 WHERE PaymentId=?";
		update.update(updatePaymentStatus,new Object[]{paymentId});
		return true;
	}

	public String getSenderAccount(String paymentId) {
		JdbcTemplate select = new JdbcTemplate(dataSource);
		String selectSenderAccount = "SELECT OriginAccount from bank.payments where PaymentId=?";
		String senderacc = select.queryForObject(selectSenderAccount,
				new Object[] { paymentId }, String.class);
		return senderacc;
	}
	
	public String getSenderAmmount(String paymentId) {
		JdbcTemplate select = new JdbcTemplate(dataSource);
		String amount = "SELECT amount from bank.payments where PaymentId=?";
		String senderacc = select.queryForObject(amount,
				new Object[] { paymentId }, String.class);
		return senderacc;
	}
	
	

}
