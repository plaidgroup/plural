package notsogood;

import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.States;

/**
 * This class and CompareToPlural2 will ideally allow us
 * to compare the performance of Plural's inference to
 * Anek's inference. The idea is just to make a program
 * with a lot of hairy control flow and then inline it
 * for comparison to Plural.
 * 
 * @author Nels E. Beckman
 *
 */
// RUN ANEK ON ME
@States({ "BORING" })
public class CompareToPlural2 {

	@Perm(requires="full(#0)",ensures="full(#0)")
	void reallyComplicatedMethod(Iterater i) {
		while (i.hasNext()) {
			m60(i);
			m50(i);
		}
		m50(i);
	}

	static private void m60(Iterater i) {
		while (i.hasNext()) {
			m59(i);
			m50(i);
		}
	}

	static private void m59(Iterater i) {
		while (i.hasNext()) {
			m58(i);
			m50(i);
		}
	}

	static private void m58(Iterater i) {
		while (i.hasNext()) {
			m57(i);
			m50(i);
		}
	}

	static private void m57(Iterater i) {
		while (i.hasNext()) {
			m56(i);
			m50(i);
		}
	}

	static private void m56(Iterater i) {
		while (i.hasNext()) {
			m55(i);
			m50(i);
		}
	}

	static private void m55(Iterater i) {
		while (i.hasNext()) {
			m54(i);
			m50(i);
		}
	}

	static private void m54(Iterater i) {
		while (i.hasNext()) {
			m53(i);
			m50(i);
		}
	}

	static private void m53(Iterater i) {
		while (i.hasNext()) {
			m52(i);
			m50(i);
		}
	}

	static private void m52(Iterater i) {
		while (i.hasNext()) {
			m51(i);
			m50(i);
		}
	}

	static private void m51(Iterater i) {
		while (i.hasNext()) {
			m49(i);
			m50(i);
		}
	}

	static private void m50(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m49(Iterater i) {
		while (i.hasNext()) {
			m47(i);
			m48(i);
		}
	}

	static private void m48(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m47(Iterater i) {
		while (i.hasNext()) {
			m45(i);
			m46(i);
		}
	}

	static private void m46(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m45(Iterater i) {
		while (i.hasNext()) {
			m43(i);
			m44(i);
		}
	}

	static private void m44(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m43(Iterater i) {
		while (i.hasNext()) {
			m41(i);
			m42(i);
		}
	}

	static private void m42(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m41(Iterater i) {
		while (i.hasNext()) {
			m39(i);
			m40(i);
		}
	}

	static private void m40(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m39(Iterater i) {
		while (i.hasNext()) {
			m37(i);
			m38(i);
		}
	}

	static private void m38(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m37(Iterater i) {
		while (i.hasNext()) {
			m35(i);
			m36(i);
		}
	}

	static private void m36(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m35(Iterater i) {
		while (i.hasNext()) {
			m33(i);
			m34(i);
		}
	}

	static private void m34(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m33(Iterater i) {
		while (i.hasNext()) {
			m31(i);
			m32(i);
		}
	}

	static private void m32(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m31(Iterater i) {
		while (i.hasNext()) {
			m29(i);
			m30(i);
		}
	}

	static private void m30(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m29(Iterater i) {
		while (i.hasNext()) {
			m27(i);
			m28(i);
		}
	}

	static private void m28(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m27(Iterater i) {
		while (i.hasNext()) {
			m25(i);
			m26(i);
		}
	}

	static private void m26(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m25(Iterater i) {
		while (i.hasNext()) {
			m23(i);
			m24(i);
		}
	}

	static 	private void m24(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m23(Iterater i) {
		while (i.hasNext()) {
			m21(i);
			m22(i);
		}
	}

	static private void m22(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m21(Iterater i) {
		while (i.hasNext()) {
			m19(i);
			m20(i);
		}
	}

	static private void m20(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m19(Iterater i) {
		while (i.hasNext()) {
			m17(i);
			m18(i);
		}
	}

	static private void m18(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m17(Iterater i) {
		while (i.hasNext()) {
			m15(i);
			m16(i);
		}
	}

	static private void m16(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m15(Iterater i) {
		while (i.hasNext()) {
			m13(i);
			m14(i);
		}
	}

	static private void m14(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m13(Iterater i) {
		while (i.hasNext()) {
			m11(i);
			m12(i);
		}
	}

	static 	private void m12(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m11(Iterater i) {
		while (i.hasNext()) {
			m9(i);
			m10(i);
		}
	}

	static private void m10(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m9(Iterater i) {
		while (i.hasNext()) {
			m7(i);
			m8(i);
		}
	}

	static private void m8(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m7(Iterater i) {
		while (i.hasNext()) {
			m5(i);
			m6(i);
		}
	}

	static private void m6(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m5(Iterater i) {
		while (i.hasNext()) {
			m3(i);
			m4(i);
		}
	}

	static private void m4(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m3(Iterater i) {
		while (i.hasNext()) {
			m1(i);
			m2(i);
		}
	}

	static private void m2(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}

	static private void m1(Iterater i) {
		while (i.hasNext()) {
			i.next();
		}
	}
}