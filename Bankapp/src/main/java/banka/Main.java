package banka;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class Main {
    private static final Connection connection;

    static {
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5433/postgres", "postgres", "postgres");
        } catch (SQLException e) {
            throw new DatabaseConnectionException();
        }
    }

    public void hesapEkle(Account account) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO account (name, surname, tcno,amount) VALUES (?, ?, ?, ?)")) {

            ps.setString(1, account.getName());
            ps.setString(2, account.getSurname());
            ps.setString(3, account.getTcno());
            ps.setInt(4, account.getAmount());
            ps.executeUpdate();
            System.out.println("hesap başarıyla oluşturuldu...");
        } catch (SQLException e) {
            System.out.println("hesap oluşturulamadı: {} "+e.getMessage());
        }
    }

    public boolean kullaniciGirisi(String accountnumber, String name, String surname) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM account WHERE name=? AND surname=? AND account_number=?")) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, surname);
            preparedStatement.setString(3, accountnumber);
            ResultSet resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            System.out.println("giriş yapılamadı: {} "+ e.getMessage());
            return false;
        }
    }

    public void paraYatir(String hesapNumarasi, double miktar) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE account SET amount = amount + ? WHERE account_number = ?")) {
            preparedStatement.setDouble(1, miktar);
            preparedStatement.setString(2, hesapNumarasi);
            preparedStatement.executeUpdate();
            System.out.println(" {} TL yatırıldı."+ miktar);
        } catch (SQLException e) {
            System.out.println("para yatırılamadı: {} "+e.getMessage());
        }
    }

    public void paraCek(String hesapNumarasi, double miktar) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE account SET amount = amount - ? WHERE account_number = ?")) {
            preparedStatement.setDouble(1, miktar);
            preparedStatement.setString(2, hesapNumarasi);
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println(" {} TL para çekildi."+ miktar);
            } else {
                System.out.println("bakiye yetersiz.");
            }
        } catch (SQLException e) {
            System.out.println("para çekilemedi: {} "+e.getMessage());
        }
    }

    public void paraaAktar(String gonderenHesapNumarasi, String hedefHesapNumarasi, double miktar) {
        try (PreparedStatement gonderenStatement = connection.prepareStatement("UPDATE account SET amount = amount - ? WHERE account_number = ?")) {
            connection.setAutoCommit(false);
            gonderenStatement.setDouble(1, miktar);
            gonderenStatement.setString(2, gonderenHesapNumarasi);
            int gonderenrowsAffected = gonderenStatement.executeUpdate();
            if (gonderenrowsAffected > 0) {
                try (PreparedStatement hedefStatement = connection.prepareStatement("UPDATE account SET amount = amount + ? WHERE account_number = ?")) {
                    hedefStatement.setDouble(1, miktar);
                    hedefStatement.setString(2, hedefHesapNumarasi);
                    hedefStatement.executeUpdate();
                    connection.commit();
                    System.out.println("{} TL {} nolu hesaptan {} nolu hesaba gönderildi"+ new Object[]{miktar, gonderenHesapNumarasi, hedefHesapNumarasi});
                }
            } else {
                connection.rollback();
                System.out.println("bakiye yetersiz.");
            }

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                System.out.println("Rollback failed: {}"+ rollbackException.getMessage());
            }
            System.out.println("para gönderilemedi: {}"+ e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                System.out.println("Error resetting auto-commit:{} "+e.getMessage());
            }
        }
    }

    public void bilgiGoster(String hesapNumarasi) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT name,surname,tcno,account_number,amount FROM account WHERE account_number=?")) {
            preparedStatement.setString(1, hesapNumarasi);
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.println(",  ");
                    String columnValue = rs.getString(i);
                    System.out.println("{}: {}"+ new Object[] {rsmd.getColumnName(i), columnValue});
                }
                System.out.println("");
            }

        } catch (SQLException e) {
            System.out.println("bilgi gösterilemedi:{} "+ e.getMessage());
        }
    }

    public void mainLoop() {
        Scanner input = new Scanner(System.in);
        int login = -1;
        String name;
        String surname;
        String tcno;
        String accountnumber;

        while (true) {
            System.out.println("1-hesap oluştur");
            System.out.println("2-giriş yap");
            System.out.println("3-çıkış");

            try {
                login = input.nextInt();
            } catch (Exception e) {
                login = -1;
            }

            switch (login) {
                case 1:
                    System.out.println("isminizi giriniz:");
                    name = input.next();
                    System.out.println("soy isminizi giriniz:");
                    surname = input.next();
                    System.out.println("tc kimlik numaranızı giriniz:");
                    tcno = input.next();
                    Account ac = new Account(name, surname, tcno, 0);
                    this.hesapEkle(ac);
                    break;

                case 2:
                    System.out.println("hesap numaranızı giriniz:");
                    accountnumber = input.next();
                    System.out.println("isminizi giriniz:");
                    name = input.next();
                    System.out.println("soy isminizi giriniz:");
                    surname = input.next();
                    boolean loginSuccess = this.kullaniciGirisi(accountnumber, name, surname);

                    while (loginSuccess) {
                        System.out.println("1-para yatırma");
                        System.out.println("2-para çekme");
                        System.out.println("3-hesaplar arası para transferi");
                        System.out.println("4-hesap bilgisi göster");
                        System.out.println("5-çıkış");
                        login = input.nextInt();

                        switch (login) {
                            case 1:
                                System.out.println("yatırmak istediğiniz miktarı giriniz:");
                                int depositAmount = input.nextInt();
                                this.paraYatir(accountnumber, depositAmount);
                                break;

                            case 2:
                                System.out.println("çekmek istediğiniz miktarı giriniz:");
                                int withdrawAmount = input.nextInt();
                                this.paraCek(accountnumber, withdrawAmount);
                                break;

                            case 3:
                                System.out.println("hedef hesap numarasını giriniz:");
                                String targetAccountNumber = input.next();
                                System.out.println("göndermek istediğiniz miktarı giriniz:");
                                int transferAmount = input.nextInt();
                                this.paraaAktar(accountnumber, targetAccountNumber, transferAmount);
                                break;

                            case 4:
                                this.bilgiGoster(accountnumber);
                                break;

                            case 5:
                                System.out.println("çıkış yapılıyor");
                                loginSuccess = false;
                                break;

                            default:
                                System.out.println("lütfen doğru değer giriniz...");
                                break;
                        }
                    }
                    break;

                case 3:
                    System.out.println("çıkış yapılıyor...");
                    return;

                default:
                    System.out.println("lütfen doğru değer giriniz...");
                    break;
            }
        }
    }


    public static void main(String[] args) {
        Main main = new Main();
        main.mainLoop();
    }
}