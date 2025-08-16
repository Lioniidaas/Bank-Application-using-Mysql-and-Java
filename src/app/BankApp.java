package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import exception.InsufficientFundsException;

public class BankApp {
	
	private Connection con;
	private Scanner sc;
	private int loggedInUserId = -1;
	private String loggedInUsername = ""; 
	
	public BankApp()throws Exception{
		Class.forName("com.mysql.cj.jdbc.Driver");
		con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank_system","root","");
		
		sc = new Scanner(System.in);
	}
	
	
	public void mainMenu() {
		while(true) {
			System.out.println("------------------ Welcome to Bank System ------------------");
			System.out.println("1. Register");
			System.out.println("2. Login");
			System.out.println("3. Exit");
			System.out.print("Enter Choice :: ");
			
			int choice = sc.nextInt();
			sc.nextLine();
			
			switch(choice) {
			case 1 -> registerUser();
			case 2 -> loginUser();
			case 3 -> {System.out.println("GoodBye!!");
					return;
			}
			default -> System.out.println("Invalid Choice");
			}
		}
	}
	
	public void registerUser() {
		System.out.println("Enter Username :: ");
		String username = sc.nextLine();
		System.out.println("Enter Password :: ");
		String password = sc.nextLine();
		String q = "call create_new_user('"+username+"','"+password+"')";
		try {
			PreparedStatement ps = con.prepareStatement(q);
			ps.executeUpdate();
			System.out.println("User Registered successfully");
		}catch(SQLException e) {
			if(e.getMessage().contains("Username already exists")) {
				System.out.println("Error !! Username already exists");
			}else {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void loginUser() {
		System.out.println("Enter Username :: ");
		String username= sc.nextLine();
		System.out.println("Enter Password :: ");
		String password =sc.nextLine();
		
		try {
			String q = "select user_id from auth where username ='"+username+"' and password = '"+password+"'" ;
			PreparedStatement ps = con.prepareStatement(q);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				loggedInUserId = rs.getInt("user_id");
				loggedInUsername = username;
				System.out.println("Login Successfully!!");
				postLoginMenu();
			}else {
				System.out.println("Invalid username or password");
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void postLoginMenu() throws Exception {
		 while(true) {
			 System.out.println("------ Welcome,  "+loggedInUsername+" --------");
			 System.out.println("1. Create Bank Account");
	         System.out.println("2. Use Existing Bank Account");
	         System.out.println("3. Logout");
	         System.out.print("Enter choice: ");
	         
	         int choice = sc.nextInt();
	         sc.nextLine();
	         
	         switch(choice) {
	         case 1 -> createBankAccount();
	         case 2 -> useExistingAccount();
	         case 3 -> {
	        	 loggedInUserId = -1;
	        	 loggedInUsername ="";
	        	 System.out.println("Logged out!!");
	        	 return;
	         }
	         default -> System.out.println("Invalid Choice!");
	         }
		 }
	}
	
	private void createBankAccount() {
		try {
			System.out.println("Available Bank :");
			String q="select bank_id , bank_name from bank";
			PreparedStatement ps = con.prepareStatement(q);
			ResultSet rs = ps.executeQuery();
			List<Integer> bankIds = new ArrayList<>();
			while(rs.next()) {
				System.out.println(rs.getInt("bank_id") + ". "+rs.getString("bank_name"));
				bankIds.add(rs.getInt("bank_id"));
			}
			System.out.println("Choose Bank ID :: ");
			int bankId = sc.nextInt();
			sc.nextLine();
			
			System.out.println("Available Branches : ");
			String p ="select branch_id , branch_name from branch where bank_id = '"+bankId+"'";
			ps = con.prepareStatement(p);
			rs = ps.executeQuery();
			List<Integer> BranchIds = new ArrayList<>();
			while(rs.next()) {
				System.out.println(rs.getInt("branch_id") + ". " + rs.getString("branch_name"));
				BranchIds.add(rs.getInt("branch_id"));
			}
			System.out.println("Choose Branch ID :: ");
			int branchId = sc.nextInt();
			sc.nextLine();
			
			System.out.println("Enter Account Holder Name :: ");
			String holderName = sc.nextLine();
			System.out.println("Enter Contact No :: ");
			String contactNo = sc.nextLine();
			
			String bankName = "";
			String r = "select bank_name from bank where bank_id = '"+bankId+"'";
			ps = con.prepareStatement(r);
			rs = ps.executeQuery();
			if(rs.next()) {
				bankName = rs.getString("bank_name");
			}
			String accNo = bankName.substring(0,Math.min(bankName.length(),4)).toUpperCase()+(int)(Math.random() * 90000 + 10000);
			
			String s = "select branch_name from branch where branch_id = '"+branchId+"'";
			ps =con.prepareStatement(s);
			rs = ps.executeQuery();
			String branchName = "";
			if(rs.next()) {
				branchName = rs.getString("branch_name");
			}
			String ifsc = branchName.substring(0,Math.min(branchName.length(),3)).toUpperCase()+ contactNo;
			
			ps=con.prepareStatement("call create_account(? , ? , ? , ? , ? , ? , ?)");
			ps.setInt(1, loggedInUserId);
			ps.setInt(2, bankId);
			ps.setInt(3, branchId);
			ps.setString(4, accNo);
			ps.setString(5, ifsc);
			ps.setString(6, holderName);
			ps.setString(7, contactNo);
			ps.executeUpdate();
			
			System.out.println("Account Created Successfully!!");
			System.out.println("Account No : "+accNo);
			System.out.println("IFSC Code : "+ifsc);
			
		}catch(SQLException e) {
			if(e.getMessage().contains("Account already exist in this bank")) {
				System.out.println("Error : You have already have an account in this bank");
			}else {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private void useExistingAccount() throws Exception {
		try {
			String p="select a.account_no,b.bank_name,br.branch_name,a.balance from account a "
					+ "join bank b on a.bank_id = b.bank_id join branch br on a.branch_id = br.branch_id where a.user_id = '"+loggedInUserId+"'";
			PreparedStatement ps = con.prepareStatement(p);
			ResultSet rs = ps.executeQuery();
			List<String> accountNos = new ArrayList<>();
			int i=1;
			System.out.println("Your Accounts : ");
			while(rs.next()) {
			 System.out.printf("%d. %s - %s | Acc No: %s | Balance: %.2f\n",
					 i, rs.getString("bank_name"), rs.getString("branch_name"),
                     rs.getString("account_no"), rs.getDouble("balance"));
			 accountNos.add(rs.getString("account_no"));
			 i++;
			}
			if(accountNos.isEmpty()) {
				System.out.println("No accounts found. Please create one first!");
				return;
			}
			System.out.println("Choose Account :");
			int accChoice = sc.nextInt();
			sc.nextLine();
			if(accChoice < 1 || accChoice > accountNos.size()) {
				System.out.println("Invalid Choice!!");
				return ;
			}
			String selectedAcc = accountNos.get(accChoice-1);
			accountOperations(selectedAcc);
		}catch(SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	private void accountOperations(String accountNo) throws Exception {
		while(true) {
			System.out.println("\n=== Operations for Account: " + accountNo + " ===");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. Check Balance");
            System.out.println("5. Back");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine();
            
            switch(choice) {
            case 1 -> deposit(accountNo);
            case 2 -> withdraw(accountNo);
            case 3 -> transfer(accountNo);
            case 4 -> checkBalance(accountNo);
            case 5 -> { return; }
            default -> System.out.println("Invalid choice!");
            }
            }

		}
	
	private void deposit(String accountNo) {
		System.out.println("Enter amount to deposit :: ");
		double amt = sc.nextDouble();
		sc.nextLine();
		System.out.println("Transaction is processing... please wait.");
	    try {
	        Thread.sleep(4000); // 4 sec 
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
		try {
			String q="call deposit_amount('"+accountNo+"',"+amt+")";
			PreparedStatement ps = con.prepareStatement(q);
			ps.executeUpdate();
			System.out.println("Deposit successful!");
		}catch(SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void withdraw(String accountNo) throws Exception {
		System.out.println("Enter amount to withdraw :: ");
		double amt = sc.nextDouble();
		sc.nextLine();
		System.out.println("Transaction is processing... please wait.");
	    try {
	        Thread.sleep(4000); // 4 sec delay AFTER entering amount
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
		try {
			String q = "call withdraw_amount('"+accountNo+"','"+amt+"')";
			PreparedStatement ps = con.prepareStatement(q);
			ps.executeUpdate();
			System.out.println("Withdrawal Successful!!");
		}catch(SQLException e) {
			if(e.getMessage().contains("Insufficient Funds")) {
				throw new InsufficientFundsException();
			}else {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private void transfer(String accountNo)throws Exception {
		System.out.println("Enter destination account no :: ");
		String destAcc = sc.nextLine();
		System.out.println("Enter amount to transfered :: ");
		double amt = sc.nextDouble();
		sc.nextLine();
		System.out.println("Transaction is processing... please wait.");
	    try {
	        Thread.sleep(4000); // 4 sec delay AFTER entering amount
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
		try {
			String q = "call transfer_amount('"+accountNo+"','"+destAcc+"','"+amt+"')";
			PreparedStatement ps = con.prepareStatement(q);
			ps.executeUpdate();
			System.out.println("Transfer Successful!");
		}catch(SQLException e) {
			if (e.getMessage().contains("Insufficient Funds")) {
                throw new InsufficientFundsException();
            } else if (e.getMessage().contains("Destination account does not exist")) {
                System.out.println("Error: Destination account does not exist!");
            } else if (e.getMessage().contains("Source account does not exist")) {
                System.out.println("Error: Source account does not exist!");
            } else {
                System.out.println(e.getMessage());
            }
		}
		
	}
	
	private void checkBalance(String accountNo) {
		try {
			System.out.println("processing... please wait.");
		    try {
		        Thread.sleep(4000); // 4 sec delay AFTER entering amount
		    } catch (InterruptedException e) {
		        Thread.currentThread().interrupt();
		    }
			String q = "call check_balance('"+accountNo+"')";
			PreparedStatement ps = con.prepareStatement(q);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				System.out.println("Current Balance : "+rs.getDouble("balance"));
			}else {
				System.out.println("Account Not Found!!");
			}
		}catch(SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	

	
	
	
	
	
	public static void main(String[] args) {
		try {
			BankApp app = new BankApp();
			app.mainMenu();
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	

}
