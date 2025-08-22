package com.fx.FxSmartApi.service.advisor.strategies;

import com.fx.FxSmartApi.model.Candle;
import java.util.*;

public class IctPrimitives {

    public static boolean isSwingHigh(List<Candle> x, int i, int L, int R){
        double hi = x.get(i).getHigh();
        for(int k=i-L;k<=i+R;k++) if(k!=i && k>=0 && k<x.size() && x.get(k).getHigh()>=hi) return false;
        return true;
    }
    public static boolean isSwingLow(List<Candle> x, int i, int L, int R){
        double lo = x.get(i).getLow();
        for(int k=i-L;k<=i+R;k++) if(k!=i && k>=0 && k<x.size() && x.get(k).getLow()<=lo) return false;
        return true;
    }

    /** basit FVG: 3-bar */
    public static class Fvg { public final int i0,i1,i2; public final boolean bullish; public final double top,bottom;
        public Fvg(int i0,int i1,int i2, boolean bullish, double top,double bottom){
            this.i0=i0; this.i1=i1; this.i2=i2; this.bullish=bullish; this.top=top; this.bottom=bottom; } }
    public static List<Fvg> detectFvg(List<Candle> c){
        List<Fvg> out = new ArrayList<>();
        for(int i=2;i<c.size();i++){
            Candle a=c.get(i-2), b=c.get(i-1), d=c.get(i);
            // bullish: low[i] > high[i-2]
            if (d.getLow() > a.getHigh()) out.add(new Fvg(i-2,i-1,i,true, d.getLow(), a.getHigh()));
            // bearish: high[i] < low[i-2]
            if (d.getHigh() < a.getLow()) out.add(new Fvg(i-2,i-1,i,false, a.getLow(), d.getHigh()));
        }
        return out;
    }

    /** basit BOS: karşıt swing seviyesinin displacement ile kırılması */
    public static class Bos { public final int index; public final boolean up; public final double level;
        public Bos(int i, boolean up, double level){ this.index=i; this.up=up; this.level=level; } }

    public static Optional<Bos> findBos(List<Candle> c, int L, int R){
        // son 100 bar içinde arayalım
        int n=c.size();
        int from=Math.max(0, n-120);
        // karşıt swingleri topla
        List<Integer> sh=new ArrayList<>(), sl=new ArrayList<>();
        for(int i=from+L;i<n-R;i++){
            if (isSwingHigh(c,i,L,R)) sh.add(i);
            if (isSwingLow(c,i,L,R))  sl.add(i);
        }
        // En son kırılımı ara
        for(int i=from+3;i<n;i++){
            double hi=c.get(i).getHigh(), lo=c.get(i).getLow();
            // down trendteki swing high kırıldı mı? -> BOS up
            for(int idx: sh) if (idx<i && lo>c.get(idx).getHigh()) return Optional.of(new Bos(i,true,c.get(idx).getHigh()));
            // up trendteki swing low kırıldı mı?  -> BOS down
            for(int idx: sl) if (idx<i && hi<c.get(idx).getLow())  return Optional.of(new Bos(i,false,c.get(idx).getLow()));
        }
        return Optional.empty();
    }

    /** Order Block: BOS'tan önceki son karşı renkli mum gövdesi (basitleştirilmiş) */
    public static class OB { public final int index; public final boolean bullish; public final double open, close;
        public OB(int i, boolean bull, double open,double close){ this.index=i; this.bullish=bull; this.open=open; this.close=close; } }
    public static Optional<OB> lastObBefore(List<Candle> c, int bosIndex, boolean bosUp){
        for(int i=bosIndex-1;i>=Math.max(0, bosIndex-20);i--){
            boolean isDown = c.get(i).getOpen() > c.get(i).getClose();
            boolean isUp   = c.get(i).getClose() > c.get(i).getOpen();
            if (bosUp && isDown) return Optional.of(new OB(i,true, c.get(i).getOpen(), c.get(i).getClose()));
            if (!bosUp && isUp)  return Optional.of(new OB(i,false, c.get(i).getOpen(), c.get(i).getClose()));
        }
        return Optional.empty();
    }

    /** OTE: dealing range fib 62–79% arasında mı (long için) / 21–38% (short için simetrik düşün) */
    public static boolean inOteLong(double price, double swingLow, double swingHigh, double min, double max){
        if (swingHigh<=swingLow) return false;
        double fib = (price - swingLow)/(swingHigh - swingLow); // 0..1
        return fib>=min && fib<=max;
    }
    public static boolean inOteShort(double price, double swingHigh, double swingLow, double min, double max){
        if (swingHigh<=swingLow) return false;
        double fib = (swingHigh - price)/(swingHigh - swingLow); // 0..1
        return fib>=min && fib<=max;
    }
}
