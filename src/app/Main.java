package app;

public class Main {

	public static void main(String[] args) {

		try {
			
			LuceneWrapper lw = new LuceneWrapper();
			lw.search();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
