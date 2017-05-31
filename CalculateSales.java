package jp.alhinc.watanabe_kenta.calculate_sales;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
public class CalculateSales {
	public static void main (String [] args) {
		
		if (args.length != 1 ) {
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		String fileReadName1 = args[0] + File.separator + "branch.lst";
		String fileReadName2 = args[0] + File.separator + "commodity.lst";
		HashMap< String, String > branch = new HashMap < String, String > () ;
		HashMap< String, Long > branchMoney = new HashMap < String, Long > () ;
		HashMap< String, Long > commodityMoney = new HashMap < String, Long > () ;
		HashMap< String, String > commodity = new HashMap < String, String > () ;
		
		
		//支店定義ファイル
		if (!fileListRead (fileReadName1, "支店", "^[0-9]{3}$", branch, branchMoney)){
			return;
		}
		if (!fileListRead (fileReadName2, "商品", "^[0-9a-zA-Z]{8}$", commodity, commodityMoney)) {
			return;
		}
		//
		//集計
		ArrayList <File> salesList = new ArrayList <File> ();

		BufferedReader br = null;
		FileReader fr = null;
		try {
			File file = new File (args[0]);
			File [] f = file.listFiles();
			for (int i = 0; i < f.length; i++) {
				if (f[i].getName().matches("^[0-9]{8}.rcd$" ) && f[i].isFile()) {
					salesList.add(f[i]);
				}
			}
			for (int i = 0; i <salesList.size(); i++) {
				String FileNumber = salesList.get(i).getName();
				String [] fileNumbers = FileNumber.split("[.]",0);
				Long number = Long.parseLong(fileNumbers[0]);
				if (number - i != 1) {
					System.out.println ("売上ファイル名が連番になっていません");
					return;
				}
			}
			String str ;
			for (int i = 0; i < salesList.size() ; i++) {
				//１つ１つ売上データのリストを作るために一回一回初期化しなければいけない
				//初期化しないと同じリストにそれぞれの売上データを全部格納してしまう
				ArrayList <String> dataList = new ArrayList <String> ();
				File rcd = salesList.get(i) ;
				fr = new FileReader (rcd);
				br = new BufferedReader (fr) ;
				while ((str = br.readLine()) != null) {

					//dataListにstrのデータを格納
					dataList.add(str);
				}
				if (dataList.size() != 3) {
					System.out.println( f[i].getName () + "のフォーマットが不正です");
					return;
				}
				if (!branch.containsKey (dataList.get(0))) {
					System.out.println( f[i].getName() + "の支店コードが不正です");
					return;
				}
				if (!commodity.containsKey (dataList.get(1))) {
					System.out.println( f[i].getName() + "の商品コードが不正です");
					return;
				}

				//整数に直す
				Long money = Long.parseLong(dataList.get(2));
				//baseでキーを使って、支店ごとの売り上げを呼び出し
				Long base1 = branchMoney.get(dataList.get(0));
				Long branchTotal = base1 + money;
				//base2で商品ごとの売り上げを呼び出し
				Long base2 = commodityMoney.get(dataList.get(1));
				Long commodityTotal = base2 + money;

				if (branchTotal > 9999999999L || commodityTotal > 9999999999L){
					System.out.println ("合計金額が10桁を超えました");
					return;
				}
				branchMoney.put(dataList.get(0), branchTotal);
				commodityMoney.put(dataList.get(1), commodityTotal);
			}
		} catch (Exception e) {
			System.out.println("予期せぬエラーが発生しました");
			return;
		}finally{
			try {
				if (br != null) {
					br.close ();
					fr.close();
				}
			} catch (IOException e) {
				System.out.println("予期せぬエラーが発生しました");
				// TODO 自動生成された catch ブロック
			}
		}

		if (!fileListOut(args[0], "branch.out", branch, branchMoney)) {
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		if (!fileListOut(args[0], "commodity.out", commodity, commodityMoney)) {
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
	}


	public static boolean fileListOut (String dirPath, String fileName, HashMap< String, String > nameMap, HashMap< String, Long > moneyMap) {

		//支店商品別集計降順出力
		List<Map.Entry<String,Long>> entries = new ArrayList<Map.Entry<String,Long>>(moneyMap.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String,Long>>() {
			public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
				return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
			}
		});
		BufferedWriter bw = null;
		try {
			File file = new File (dirPath, fileName);
			FileWriter fw = new FileWriter (file);
			bw = new BufferedWriter (fw);
			String rn = System.getProperty("line.separator");
			for (Entry <String, Long> s : entries) {
				bw.write(s.getKey() + "," + nameMap.get(s.getKey()) + "," + moneyMap.get(s.getKey()) + rn);
			}
		} catch (Exception e) {
			return false;
		} finally{
			try {
				if (bw != null) {
					bw.close () ;
				}
			} catch (IOException e) {
				System.out.println("予期せぬエラーが発生しました");
				// TODO 自動生成された catch ブロック
			}
		}
		return true;
	}

	public static boolean fileListRead (String dirFilePath, String branchCommodity,String condition, HashMap< String, String > nameMap, HashMap< String, Long > moneyMap) {
		BufferedReader br = null;
		
		
		File file = new File (dirFilePath);
		if (!file.exists ()) {
			System.out.println(branchCommodity + "定義ファイルが存在しません");
			return false;
		} 

		try {
			FileReader fr = new FileReader (file);
			br = new BufferedReader (fr) ;

			String str ;
			while (( str = br.readLine()) != null) {
				String [] branchNumbers = str.split(",");
				String branchNumber = branchNumbers [0] ;
				if (!branchNumber.matches (condition) || branchNumbers.length != 2) {
					System.out.println(branchCommodity + "定義ファイルのフォーマットが不正です");
					return false;
				}
				nameMap.put(branchNumbers[0], branchNumbers[1]);
				moneyMap.put(branchNumbers[0], 0L);
			}
		} catch (IOException e) {
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}finally{
			try {
				br.close () ;
			} catch (IOException e) {
				System.out.println("予期せぬエラーが発生しました");
				// TODO 自動生成された catch ブロック
			}
		}
		return true;
	}
}
