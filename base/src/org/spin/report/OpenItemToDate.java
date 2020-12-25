/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2017 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpcya.com                                 *
 *****************************************************************************/
package org.spin.report;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * @author Yamel Senih 2017-06-11
 * Add Open Item to Date, you can choice between date invoiced or accounting date
 */
public class OpenItemToDate extends OpenItemToDateAbstract {
	
	/**	Start Time				*/
	private long 				m_start = System.currentTimeMillis();
	
	
	@Override
	protected String doIt() throws SQLException{
		StringBuffer sql = new StringBuffer();
		StringBuffer whereClause = new StringBuffer();
		//	Organization
		if(getOrgId() != 0) {
			whereClause.append(" AND i.AD_Org_ID = ?");
		}
		//	Business Partner Group
		if(getBPGroupId() != 0) {
			whereClause.append(" AND i.C_BP_Group_ID = ?");
		}
		//	Business Partner
		if(getBPartnerId() != 0) {
			whereClause.append(" AND i.C_BPartner_ID = ?");
		}
		//	Document Type
		if(getDocTypeId() != 0) {
			whereClause.append(" AND i.C_DocType_ID = ?");
		}
		//	Collection Status
		if(!Util.isEmpty(getInvoiceCollectionType())) {
			whereClause.append(" AND i.InvoiceCollectionType = ?");
		}
		//	Date Invoiced From
		if(getDateInvoiced() != null) {
			whereClause.append(" AND i.DateInvoiced >= ?");
		}
		//	Date Invoiced To
		if(getDateInvoicedTo() != null) {
			whereClause.append(" AND i.DateInvoiced <= ?");
		}
		//	Accounting Date From
		if(getDateAcct() != null) {
			whereClause.append(" AND i.DateAcct >= ?");
		}
		//	Accounting Date To
		if(getDateAcctTo() != null) {
			whereClause.append(" AND i.DateAcct <= ?");
		}
		//	Insert
		sql.append("INSERT INTO T_OpenItemToDate(AD_Org_ID, AD_Client_ID, DocumentNo, C_Invoice_ID, "
				+ "C_Order_ID, C_BPartner_ID, IsSOTrx, DateInvoiced, "
				+ "DateAcct, NetDays, DueDate, DaysDue, DiscountDate, "
				+ "DiscountAmt, GrandTotal, PaidAmt, OpenAmt, "
				+ "C_Currency_ID, C_ConversionType_ID, C_PaymentTerm_ID, "
				+ "IsPayScheduleValid, C_InvoicePaySchedule_ID, InvoiceCollectionType, "
				+ "C_Campaign_ID, C_Project_ID, C_Activity_ID, C_DocType_ID, DateTo, "
				+ "AD_PInstance_ID, Created, Updated, CreatedBy, UpdatedBy) ");
		
		//	Main Select
		sql.append("SELECT i.AD_Org_ID, i.AD_Client_ID, i.DocumentNo, i.C_Invoice_ID, "
				+ "i.C_Order_ID, i.C_BPartner_ID, i.IsSOTrx, i.DateInvoiced, "
				+ "i.DateAcct, i.NetDays, i.DueDate, i.DaysDue, i.DiscountDate, "
				+ "i.DiscountAmt, i.GrandTotal, i.PaidAmt, i.OpenAmt, "
				+ "i.C_Currency_ID, i.C_ConversionType_ID, i.C_PaymentTerm_ID, "
				+ "i.IsPayScheduleValid, i.C_InvoicePaySchedule_ID, i.InvoiceCollectionType, "
				+ "i.C_Campaign_ID, i.C_Project_ID, i.C_Activity_ID, i.C_DocType_ID, ?, ")
				//	Instance, Created, Updated, CreatedBy, UpdatedBy
				.append(getAD_PInstance_ID()).append(", getdate(), getdate(), ").append(getAD_User_ID()).append(", ").append(getAD_User_ID()).append(" ");
		//	Without Payment Schedule
		sql.append("FROM (SELECT i.AD_Org_ID, i.AD_Client_ID,	"
				+ "i.DocumentNo, i.C_Invoice_ID, i.C_Order_ID, i.C_BPartner_ID, i.IsSOTrx, "
				+ "i.DateInvoiced, i.DateAcct, "
				+ "p.NetDays, "
				+ "paymentTermDueDate(i.C_PaymentTerm_ID, i.DateInvoiced) AS DueDate, "
				+ "paymentTermDueDays(i.C_PaymentTerm_ID, i.DateInvoiced, ?) AS DaysDue, "
				+ "addDays(i.DateInvoiced,p.DiscountDays) AS DiscountDate, "
				+ "ROUND(i.GrandTotal*p.Discount/100,2) AS DiscountAmt, "
				+ "i.GrandTotal, "
				+ "invoicePaidToDate(i.C_Invoice_ID, i.C_Currency_ID, 1, ?) AS PaidAmt, "
				+ "invoiceOpenToDate(i.C_Invoice_ID, 0, ?) AS OpenAmt, "
				+ "i.C_Currency_ID, i.C_ConversionType_ID, "
				+ "i.C_PaymentTerm_ID, "
				+ "i.IsPayScheduleValid, cast(null as numeric) AS C_InvoicePaySchedule_ID, i.InvoiceCollectionType, "
				+ "i.C_Campaign_ID, i.C_Project_ID, i.C_Activity_ID, i.C_DocType_ID "
				+ "FROM RV_C_Invoice i "
				+ "INNER JOIN C_PaymentTerm p ON (i.C_PaymentTerm_ID = p.C_PaymentTerm_ID) "
				+ "WHERE invoiceOpenToDate(i.C_Invoice_ID, 0, ?) <> 0 "
				+ "AND (i.invoicecollectiontype is null or i.invoicecollectiontype not in ( 'H') ) "
				+ "AND i.IsPayScheduleValid<>'Y' "
				+ "AND i.processed = 'Y'  ");
		//	Union
		sql.append("UNION ");
		//	With Payment Schedule
		sql.append("SELECT i.AD_Org_ID, i.AD_Client_ID, "
				+ "i.DocumentNo, i.C_Invoice_ID, i.C_Order_ID, i.C_BPartner_ID, i.IsSOTrx, "
				+ "i.DateInvoiced, i.DateAcct, "
				+ "daysBetween(ips.DueDate,i.DateInvoiced) AS NetDays, "
				+ "ips.DueDate, "
				+ "daysBetween(?, ips.DueDate) AS DaysDue, "
				+ "ips.DiscountDate, "
				+ "ips.DiscountAmt, "
				+ "ips.DueAmt AS GrandTotal, "
				+ "invoicePaidToDate(i.C_Invoice_ID, i.C_Currency_ID, 1, ?) AS PaidAmt, "
				+ "invoiceOpenToDate(i.C_Invoice_ID, ips.C_InvoicePaySchedule_ID, ?) AS OpenAmt, "
				+ "i.C_Currency_ID, i.C_ConversionType_ID, "
				+ "i.C_PaymentTerm_ID, "
				+ "i.IsPayScheduleValid, ips.C_InvoicePaySchedule_ID, i.InvoiceCollectionType, "
				+ "i.C_Campaign_ID, i.C_Project_ID, i.C_Activity_ID, i.C_DocType_ID "
				+ "FROM RV_C_Invoice i "
				+ "INNER JOIN C_InvoicePaySchedule ips ON (i.C_Invoice_ID = ips.C_Invoice_ID)	"
				+ "WHERE invoiceOpenToDate(i.C_Invoice_ID, ips.C_InvoicePaySchedule_ID, ? ) <> 0 "
				+ "AND i.IsPayScheduleValid='Y' "
				+ "AND (i.invoicecollectiontype is null or i.invoicecollectiontype not in ( 'H')) "
				+ "AND i.processed = 'Y'  "
				+ "AND ips.IsValid='Y' ) i ");
		//	Sales Order Transaction
		sql.append("WHERE i.IsSOTrx = ").append(isSOTrx()? "'Y'": "'N'");
		sql.append("AND i.AD_Client_ID= ").append(Env.getAD_Client_ID(getCtx()));
		sql.append(whereClause);
		//	
		log.fine("SQL = " + sql.toString());
		//	Prepare statement
		PreparedStatement pstmtInsert = DB.prepareStatement (sql.toString(), get_TrxName());
		int i = 1;
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		//	Organization
		if(getOrgId() != 0) {
			pstmtInsert.setInt(i++, getOrgId());
		}
		//	Business Partner Group
		if(getBPGroupId() != 0) {
			pstmtInsert.setInt(i++, getBPGroupId());
		}
		//	Business Partner
		if(getBPartnerId() != 0) {
			pstmtInsert.setInt(i++, getBPartnerId());
		}
		//	Document Type
		if(getDocTypeId() != 0) {
			pstmtInsert.setInt(i++, getDocTypeId());
		}
		//	Collection Status
		if(!Util.isEmpty(getInvoiceCollectionType())) {
			pstmtInsert.setString(i++, getInvoiceCollectionType());
		}
		//	Date Invoiced From
		if(getDateInvoiced() != null) {
			pstmtInsert.setTimestamp(i++, getDateInvoiced());
		}
		//	Date Invoiced To
		if(getDateInvoicedTo() != null) {
			pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		}
		//	Accounting Date From
		if(getDateAcct() != null) {
			pstmtInsert.setTimestamp(i++, getDateAcct());
		}
		//	Accounting Date To
		if(getDateAcctTo() != null) {
			pstmtInsert.setTimestamp(i++, getDateAcctTo());
		}
		int noInserts = pstmtInsert.executeUpdate();
		log.fine((System.currentTimeMillis() - m_start) + " ms");
		//	
		insertOpenPayments();
		return "@Created@ = " + noInserts;
	}	//	doIt
	
	private void insertOpenPayments()  throws SQLException
	{
		StringBuffer sql = new StringBuffer();
		StringBuffer whereClause = new StringBuffer();
		//	Organization
		if(getOrgId() != 0) {
			whereClause.append(" AND p.AD_Org_ID = ?");
		}
		//	Business Partner Group
		if(getBPGroupId() != 0) {
			whereClause.append(" AND bp.C_BP_Group_ID = ?");
		}
		//	Business Partner
		if(getBPartnerId() != 0) {
			whereClause.append(" AND p.C_BPartner_ID = ?");
		}
		//	Document Type
		
		
		//	Date Invoiced From
		if(getDateInvoiced() != null) {
			whereClause.append(" AND p.DateTrx >= ?");
		}
		//	Date Invoiced To
		if(getDateInvoicedTo() != null) {
			whereClause.append(" AND p.DateTrx  <= ?");
		}
		//	Accounting Date From
		if(getDateAcct() != null) {
			whereClause.append(" AND p.DateAcct >= ?");
		}
		//	Accounting Date To
		if(getDateAcctTo() != null) {
			whereClause.append(" AND p.DateAcct <= ?");
		}
		sql.append("INSERT INTO T_OpenItemToDate(AD_Org_ID, AD_Client_ID, DocumentNo, C_Invoice_ID, "
				+ "C_Order_ID, C_BPartner_ID, IsSOTrx, DateInvoiced, "
				+ "DateAcct, NetDays, DueDate, DaysDue, DiscountDate, "
				+ "DiscountAmt, GrandTotal, PaidAmt, OpenAmt, "
				+ "C_Currency_ID, C_ConversionType_ID, C_PaymentTerm_ID, "
				+ "IsPayScheduleValid, C_InvoicePaySchedule_ID, InvoiceCollectionType, "
				+ "C_Campaign_ID, C_Project_ID, C_Activity_ID, C_DocType_ID, DateTo, "
				+ "AD_PInstance_ID, Created, Updated, CreatedBy, UpdatedBy) ");
		sql.append("SELECT p.AD_Org_ID, p.AD_Client_ID,	"
				+ "p.DocumentNo, null, p.C_Order_ID, p.C_BPartner_ID, p.Isreceipt, "
				+ "p.datetrx, p.DateAcct, "
				+ "null, "
				+ "null AS DueDate, "
				+ "null AS DaysDue, "
				+ "null AS DiscountDate, "
				+ "null AS DiscountAmt, "
				+ "null as grandtotal, "
				+ "p.payamt - paymentavailableToDate(c_Payment_ID, ?) as PaidAmt, "
				+ "null AS OpenAmt, "
				+ "p.C_Currency_ID, p.C_ConversionType_ID, "
				+ "null as c_Paymentterm_ID, 'Y' as IsPayScheduleValid, "
				+ "null AS C_InvoicePaySchedule_ID,  null as InvoiceCollectionType, "
				+ "p.C_Campaign_ID, p.C_Project_ID, p.C_Activity_ID, p.C_DocType_ID, ?, ")
				//	Instance, Created, Updated, CreatedBy, UpdatedBy
		.append(getAD_PInstance_ID()).append(", getdate(), getdate(), ").append(getAD_User_ID()).append(", ").append(getAD_User_ID())
		.append(" ")
		.append( "	  from c_Payment_V p ")
		.append( "	  INNER JOIN C_BPartner bp on p.C_BPartner_ID=bp.C_BPartner_ID ")
		.append("     AND p.datetrx between ? and ?")
		.append( "	  where paymentavailableToDate(c_Payment_ID, ?) <> 0")
		.append(" AND p.isreceipt = ").append(isSOTrx()? "'Y'": "'N'")
		.append(" AND p.AD_Client_ID=").append(Env.getAD_Client_ID(getCtx()));
		sql.append(whereClause);
		PreparedStatement pstmtInsert = DB.prepareStatement (sql.toString(), get_TrxName());
		int i = 1;
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		//	Organization
		if(getOrgId() != 0) {
			pstmtInsert.setInt(i++, getOrgId());
		}
		//	Business Partner Group
		if(getBPGroupId() != 0) {
			pstmtInsert.setInt(i++, getBPGroupId());
		}
		//	Business Partner
		if(getBPartnerId() != 0) {
			pstmtInsert.setInt(i++, getBPartnerId());
		}
		//	Date Invoiced From
		if(getDateInvoiced() != null) {
			pstmtInsert.setTimestamp(i++, getDateInvoiced());
		}
		//	Date Invoiced To
		if(getDateInvoicedTo() != null) {
			pstmtInsert.setTimestamp(i++, getDateInvoicedTo());
		}
		//	Accounting Date From
		if(getDateAcct() != null) {
			pstmtInsert.setTimestamp(i++, getDateAcct());
		}
		//	Accounting Date To
		if(getDateAcctTo() != null) {
			pstmtInsert.setTimestamp(i++, getDateAcctTo());
		}
		//	Execute Query for insert
		int noInserts = pstmtInsert.executeUpdate();

	}

}
