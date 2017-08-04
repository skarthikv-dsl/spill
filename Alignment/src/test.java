import java.math.BigDecimal;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		test obj = new test();
		float old_val = obj.roundToDouble(1.5555555f, 3);
		float zero = 1.0f;
		float new_val = 1.55600f;
		if(zero == 1.0)
			System.out.println("Same");
		else
			System.out.println("Different");
//
//		if(old_val == new_val)
//			System.out.println("Same");
	}
	


	public static float roundToDouble(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }


}
