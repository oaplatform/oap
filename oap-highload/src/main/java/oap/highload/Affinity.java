package oap.highload;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Affinity {
    @Getter
    private final int[] cpus;
    private final AtomicInteger position = new AtomicInteger();

    public Affinity( String cpu ) {
        log.info( "cpu {}", cpu );

        if( cpu.trim().equals( "*" ) ) {
            cpus = new int[0];
        } else {
            String[] split = cpu.split( "," );

            ArrayList<Integer> cpus = new ArrayList<>();

            for( var n : split ) {
                String nTrimmed = n.trim();
                if( nTrimmed.endsWith( "+" ) ) {
                    for( int i = Integer.parseInt( nTrimmed.substring( 0, nTrimmed.length() - 1 ) ); i < Runtime.getRuntime().availableProcessors(); i++ ) {
                        cpus.add( i );
                    }
                } else {
                    String[] range = n.split( "-" );

                    if( range.length > 1 ) {
                        int start = Integer.parseInt( range[0].trim() );
                        int end = Integer.parseInt( range[1].trim() );

                        for( int i = start; i <= end; i++ ) {
                            cpus.add( i );
                        }

                    } else {
                        cpus.add( Integer.parseInt( nTrimmed ) );
                    }
                }
            }

            this.cpus = cpus.stream().mapToInt( i -> i ).toArray();
        }
    }

    public static Affinity any() {
        return new Affinity( "*" );
    }

    public void set() {
        if( isEnabled() ) {
            int cpuIndex = position.getAndUpdate( index -> index >= cpus.length ? 0 : index + 1 );
            int cpu = cpus[cpuIndex];
            log.trace( "affinity -> {}", cpu );
            net.openhft.affinity.Affinity.setAffinity( cpu );
        }
    }

    public boolean isEnabled() {
        return cpus.length > 0;
    }
}
