import java.math.BigDecimal;
import java.util.ArrayList;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		test obj = new test();
		float old_val = obj.roundToDouble(1.5555555f, 3);
		float zero = 1.0f;
		
		ArrayList<Double> a = new ArrayList<Double>(null);
		float var1 = (float)1.55;
		float var2 = 7f;
		float new_val = (float) (var1*var2);
//		System.out.println(var1);
//		System.out.println(var2);
		System.out.println(1f*1f);
		System.out.println(new_val);
		if((float)new_val == 1)
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
