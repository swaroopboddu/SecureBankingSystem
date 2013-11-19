package com.sundevil.tryouts.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.sundevil.db.paymentmanagement.IDBConnectionPayment;
import com.sundevil.domain.IAccount;
import com.sundevil.domain.ITransaction;
import com.sundevil.domain.implementation.Account;
import com.sundevil.domain.implementation.CertificateFormBean;
import com.sundevil.domain.implementation.Transaction;
import com.sundevil.security.transactionmanagement.ITransactionManager;
import com.sundevil.service.PKIService;
import com.sundevil.service.accountmanagement.IAccountManager;

@Controller
public class PKIController {

	@Autowired
	private PKIService pkiService;

	@Autowired
	private IAccountManager accManager;

	@Autowired
	private ITransactionManager transManager;

	@Autowired
	private IDBConnectionPayment payment;
	private static final Logger logger = LoggerFactory.getLogger(PKIController.class);

	@RequestMapping(value = "/ext/pki/makePayments", method = RequestMethod.GET)
	public ModelAndView enterMessageDetails(
			@ModelAttribute("certificateFormBean") CertificateFormBean certificateFormBean,
			BindingResult result, ModelMap model, Principal principal) throws Exception{
		String userName = principal.getName();
		if (certificateFormBean == null) {
			certificateFormBean = new CertificateFormBean();
		}
		model.put("certificateFormBean", certificateFormBean);
		return new ModelAndView("pki/makePayments", model);
	}

	@RequestMapping(value = "/ext/pki/sendPayment", method = RequestMethod.POST)
	public ModelAndView sendCertificate(
			@ModelAttribute("certificateFormBean") CertificateFormBean certificateFormBean,
			BindingResult result, ModelMap model, Principal principal) throws Exception{

		String userName = principal.getName();
		boolean emailSent = false;
		String merchantName = certificateFormBean.getDestinationUserName();
		List<IAccount> acc = accManager.getAllBankAccounts(merchantName);
		boolean b = false;
		for (IAccount account : acc) {
			if (account.getType().equals("MERCHANT_ACCOUNT")) {
				b = true;
				break;
			}
		}
		String sourceAccountId = certificateFormBean.getDestinationEmailId();
		String ammount = certificateFormBean.getMessage();
		List<IAccount> listOfAccounts = accManager.getAllBankAccounts(principal
				.getName());
		Account a = new Account();
		a.setAccountId(sourceAccountId);
		String regex = "^[0-9]+$";
		if (!listOfAccounts.contains(a)
				&& ammount.matches(regex)
				&& !b
				&& Long.parseLong(accManager.getBalance(sourceAccountId)) < Long
						.parseLong(ammount)) {
			return new ModelAndView("/ext/pki/makePayments");
		}
		else{
		try {
			String paymentId = ("PAYMENT_" + UUID.randomUUID());
			certificateFormBean = pkiService.generateCertificate(paymentId);
			emailSent = pkiService.sendCertificate(merchantName, ammount,
					sourceAccountId, paymentId);
			System.out.println("enter\n" + paymentId);
			payment.createPayment(paymentId, sourceAccountId, ammount);
			Transaction t = new Transaction();
			t.setOriginAccount(sourceAccountId);
			t.setDestAccount(sourceAccountId);
			t.setAmount(ammount);
			transManager.makeWithdraw(t);
		} catch (Exception e) {
			logger.info("Exception : " + e);
			e.printStackTrace();
		}
		}
		if (emailSent) {
			model.addAttribute(
					"certificateStatusMessage",
					"Certificate Sent To The Desired User. An E-Mail Is Sent To Notify The Desired User");
		} else {
			model.addAttribute(
					"errorMessage",
					"Certificate And E-Mail Could Not Be Sent. You Might Have Entered Incorrect E-Mail-Id/User-Name Combination. Try Again");

		}
		if (certificateFormBean == null) {
			certificateFormBean = new CertificateFormBean();
		}
		model.put("certificateFormBean", certificateFormBean);
		return new ModelAndView("redirect:/ext/pki/makePayments", model);

	}

	@RequestMapping(value = "/ext/pki/verifyPayment", method = RequestMethod.GET)
	public ModelAndView verifyCertificate(
			@ModelAttribute("certificateFormBean") CertificateFormBean certificateFormBean,
			BindingResult result, ModelMap model, Principal principal) throws Exception {
		String userName = principal.getName();
		boolean isVerified = false;
		String senderUserName = certificateFormBean.getSenderUserName();
		try {
			certificateFormBean = pkiService.obtainCertificateForVerification(
					senderUserName, userName);
			isVerified = pkiService.verifyCertificate(senderUserName, userName);
			// swaroop code for updating db
			payment.updatePayment(senderUserName);
			List<IAccount> a = accManager.getAllBankAccounts(userName);
			String dest = null;
			for (IAccount acc : a) {
				(acc.getType()).equals("MERCHANT_ACCOUNT");
				dest = acc.getAccountId();
			}
			if (dest != null) {
				String ammont = payment.getSenderAmmount(senderUserName);
				ITransaction t = new Transaction();
				t.setDestAccount(dest);
				t.setAmount(ammont);
				transManager.makeDeposit(t);
			} else {
				model.addAttribute("errorMessage",
						"Dont have a merchant accout");
			}
		} catch (Exception e) {
			System.out.println("Exception : " + e);
		}
		System.out.println(isVerified);
		if (isVerified) {
			model.addAttribute("certificateStatusMessage",
					"Certificate is genuine and amount credited to your account");
		} else {
			model.addAttribute("errorMessage",
					"Certificate is not valid !! Try Again.");
		}
		if (certificateFormBean == null) {
			certificateFormBean = new CertificateFormBean();
		}
		model.put("certificateFormBean", certificateFormBean);
		return new ModelAndView("pki/verifyPayment", model);

	}

}