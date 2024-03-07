package oap.util;

import oap.benchmark.Benchmark;
import org.openjdk.jol.info.GraphLayout;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test( enabled = false )
public class ObjectSizeCalculatorPerformance {
    public static final int SAMPLES = 10000;
    public static final int COUNT = 100;

    @Test( enabled = false )
    public void testStringSize() {
        String thai = "utf8 Thai: เป็นมนุษย์สุดประเสริฐเลิศคุณค่า กว่าบรรดาฝูงสัตว์เดรัจฉาน"
                + "จงฝ่าฟันพัฒนาวิชาการ อย่าล้างผลาญฤๅเข่นฆ่าบีฑาใคร"
                + "ไม่ถือโทษโกรธแช่งซัดฮึดฮัดด่า หัดอภัยเหมือนกีฬาอัชฌาสัย"
                + "ปฏิบัติประพฤติกฎกำหนดใจ พูดจาให้จ๊ะๆ จ๋าๆ น่าฟังเอย ฯ";

        // JAMM-based
        // benchmarking ObjectSizeCalculatorPerformanceTest#jamm-based: avg time 772 usec, avg rate 2021,4544 action/s

        // JOL-based ( 2.6 times faster than JAMM )
        // benchmarking ObjectSizeCalculatorPerformanceTest#jol-based: avg time 293 usec, avg rate 5322,4414 action/s
        assertThat( GraphLayout.parseInstance( thai ).totalSize() ).isEqualTo( 496 );
        Benchmark.benchmark( "jol-based", SAMPLES, () -> {
            var res = 0;
            for( var i = 0; i < COUNT; i++ ) {
                res += GraphLayout.parseInstance( Pair.__( i, "เป็" ) ).totalSize();
            }
            assertThat( res ).isEqualTo( 8800 );
        } ).run();

    }
}
