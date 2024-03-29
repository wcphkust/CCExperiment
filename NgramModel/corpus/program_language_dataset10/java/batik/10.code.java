package org.apache.batik.anim;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.batik.anim.timing.TimedDocumentRoot;
import org.apache.batik.anim.timing.TimedElement;
import org.apache.batik.anim.timing.TimegraphListener;
import org.apache.batik.anim.values.AnimatableValue;
import org.apache.batik.dom.anim.AnimationTarget;
import org.apache.batik.dom.anim.AnimationTargetListener;
import org.apache.batik.util.DoublyIndexedTable;
import org.w3c.dom.Document;
public abstract class AnimationEngine {
    public static final short ANIM_TYPE_XML   = 0;
    public static final short ANIM_TYPE_CSS   = 1;
    public static final short ANIM_TYPE_OTHER = 2;
    protected Document document;
    protected TimedDocumentRoot timedDocumentRoot;
    protected long pauseTime;
    protected HashMap targets = new HashMap();
    protected HashMap animations = new HashMap();
    protected Listener targetListener = new Listener();
    public AnimationEngine(Document doc) {
        this.document = doc;
        timedDocumentRoot = createDocumentRoot();
    }
    public void dispose() {
        Iterator i = targets.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            AnimationTarget target = (AnimationTarget) e.getKey();
            TargetInfo info = (TargetInfo) e.getValue();
            Iterator j = info.xmlAnimations.iterator();
            while (j.hasNext()) {
                DoublyIndexedTable.Entry e2 =
                    (DoublyIndexedTable.Entry) j.next();
                String namespaceURI = (String) e2.getKey1();
                String localName = (String) e2.getKey2();
                Sandwich sandwich = (Sandwich) e2.getValue();
                if (sandwich.listenerRegistered) {
                    target.removeTargetListener(namespaceURI, localName, false,
                                                targetListener);
                }
            }
            j = info.cssAnimations.entrySet().iterator();
            while (j.hasNext()) {
                Map.Entry e2 = (Map.Entry) j.next();
                String propertyName = (String) e2.getKey();
                Sandwich sandwich = (Sandwich) e2.getValue();
                if (sandwich.listenerRegistered) {
                    target.removeTargetListener(null, propertyName, true,
                                                targetListener);
                }
            }
        }
    }
    public void pause() {
        if (pauseTime == 0) {
            pauseTime = System.currentTimeMillis();
        }
    }
    public void unpause() {
        if (pauseTime != 0) {
            Calendar begin = timedDocumentRoot.getDocumentBeginTime();
            int dt = (int) (System.currentTimeMillis() - pauseTime);
            begin.add(Calendar.MILLISECOND, dt);
            pauseTime = 0;
        }
    }
    public boolean isPaused() {
        return pauseTime != 0;
    }
    public float getCurrentTime() {
        return timedDocumentRoot.getCurrentTime();
    }
    public float setCurrentTime(float t) {
        boolean p = pauseTime != 0;
        unpause();
        Calendar begin = timedDocumentRoot.getDocumentBeginTime();
        float now =
            timedDocumentRoot.convertEpochTime(System.currentTimeMillis());
        begin.add(Calendar.MILLISECOND, (int) ((now - t) * 1000));
        if (p) {
            pause();
        }
        return tick(t, true);
    }
    public void addAnimation(AnimationTarget target, short type, String ns,
                             String an, AbstractAnimation anim) {
        timedDocumentRoot.addChild(anim.getTimedElement());
        AnimationInfo animInfo = getAnimationInfo(anim);
        animInfo.type = type;
        animInfo.attributeNamespaceURI = ns;
        animInfo.attributeLocalName = an;
        animInfo.target = target;
        animations.put(anim, animInfo);
        Sandwich sandwich = getSandwich(target, type, ns, an);
        if (sandwich.animation == null) {
            anim.lowerAnimation = null;
            anim.higherAnimation = null;
        } else {
            sandwich.animation.higherAnimation = anim;
            anim.lowerAnimation = sandwich.animation;
            anim.higherAnimation = null;
        }
        sandwich.animation = anim;
        if (anim.lowerAnimation == null) {
            sandwich.lowestAnimation = anim;
        }
    }
    public void removeAnimation(AbstractAnimation anim) {
        timedDocumentRoot.removeChild(anim.getTimedElement());
        AbstractAnimation nextHigher = anim.higherAnimation;
        if (nextHigher != null) {
            nextHigher.markDirty();
        }
        moveToBottom(anim);
        if (anim.higherAnimation != null) {
            anim.higherAnimation.lowerAnimation = null;
        }
        AnimationInfo animInfo = getAnimationInfo(anim);
        Sandwich sandwich = getSandwich(animInfo.target, animInfo.type,
                                        animInfo.attributeNamespaceURI,
                                        animInfo.attributeLocalName);
        if (sandwich.animation == anim) {
            sandwich.animation = null;
            sandwich.lowestAnimation = null;
            sandwich.shouldUpdate = true;
        }
    }
    protected Sandwich getSandwich(AnimationTarget target, short type,
                                   String ns, String an) {
        TargetInfo info = getTargetInfo(target);
        Sandwich sandwich;
        if (type == ANIM_TYPE_XML) {
            sandwich = (Sandwich) info.xmlAnimations.get(ns, an);
            if (sandwich == null) {
                sandwich = new Sandwich();
                info.xmlAnimations.put(ns, an, sandwich);
            }
        } else if (type == ANIM_TYPE_CSS) {
            sandwich = (Sandwich) info.cssAnimations.get(an);
            if (sandwich == null) {
                sandwich = new Sandwich();
                info.cssAnimations.put(an, sandwich);
            }
        } else {
            sandwich = (Sandwich) info.otherAnimations.get(an);
            if (sandwich == null) {
                sandwich = new Sandwich();
                info.otherAnimations.put(an, sandwich);
            }
        }
        return sandwich;
    }
    protected TargetInfo getTargetInfo(AnimationTarget target) {
        TargetInfo info = (TargetInfo) targets.get(target);
        if (info == null) {
            info = new TargetInfo();
            targets.put(target, info);
        }
        return info;
    }
    protected AnimationInfo getAnimationInfo(AbstractAnimation anim) {
        AnimationInfo info = (AnimationInfo) animations.get(anim);
        if (info == null) {
            info = new AnimationInfo();
            animations.put(anim, info);
        }
        return info;
    }
    protected static final Map.Entry[] MAP_ENTRY_ARRAY = new Map.Entry[0];
    protected float tick(float time, boolean hyperlinking) {
        float waitTime = timedDocumentRoot.seekTo(time, hyperlinking);
        Map.Entry[] targetEntries =
            (Map.Entry[]) targets.entrySet().toArray(MAP_ENTRY_ARRAY);
        for (int i = 0; i < targetEntries.length; i++) {
            Map.Entry e = targetEntries[i];
            AnimationTarget target = (AnimationTarget) e.getKey();
            TargetInfo info = (TargetInfo) e.getValue();
            Iterator j = info.xmlAnimations.iterator();
            while (j.hasNext()) {
                DoublyIndexedTable.Entry e2 =
                    (DoublyIndexedTable.Entry) j.next();
                String namespaceURI = (String) e2.getKey1();
                String localName = (String) e2.getKey2();
                Sandwich sandwich = (Sandwich) e2.getValue();
                if (sandwich.shouldUpdate ||
                        sandwich.animation != null
                            && sandwich.animation.isDirty) {
                    AnimatableValue av = null;
                    boolean usesUnderlying = false;
                    AbstractAnimation anim = sandwich.animation;
                    if (anim != null) {
                        av = anim.getComposedValue();
                        usesUnderlying =
                            sandwich.lowestAnimation.usesUnderlyingValue();
                        anim.isDirty = false;
                    }
                    if (usesUnderlying && !sandwich.listenerRegistered) {
                        target.addTargetListener(namespaceURI, localName, false,
                                                 targetListener);
                        sandwich.listenerRegistered = true;
                    } else if (!usesUnderlying && sandwich.listenerRegistered) {
                        target.removeTargetListener(namespaceURI, localName,
                                                    false, targetListener);
                        sandwich.listenerRegistered = false;
                    }
                    target.updateAttributeValue(namespaceURI, localName, av);
                    sandwich.shouldUpdate = false;
                }
            }
            j = info.cssAnimations.entrySet().iterator();
            while (j.hasNext()) {
                Map.Entry e2 = (Map.Entry) j.next();
                String propertyName = (String) e2.getKey();
                Sandwich sandwich = (Sandwich) e2.getValue();
                if (sandwich.shouldUpdate ||
                        sandwich.animation != null
                            && sandwich.animation.isDirty) {
                    AnimatableValue av = null;
                    boolean usesUnderlying = false;
                    AbstractAnimation anim = sandwich.animation;
                    if (anim != null) {
                        av = anim.getComposedValue();
                        usesUnderlying =
                            sandwich.lowestAnimation.usesUnderlyingValue();
                        anim.isDirty = false;
                    }
                    if (usesUnderlying && !sandwich.listenerRegistered) {
                        target.addTargetListener(null, propertyName, true,
                                                 targetListener);
                        sandwich.listenerRegistered = true;
                    } else if (!usesUnderlying && sandwich.listenerRegistered) {
                        target.removeTargetListener(null, propertyName, true,
                                                    targetListener);
                        sandwich.listenerRegistered = false;
                    }
                    if (usesUnderlying) {
                        target.updatePropertyValue(propertyName, null);
                    }
                    if (!(usesUnderlying && av == null)) {
                        target.updatePropertyValue(propertyName, av);
                    }
                    sandwich.shouldUpdate = false;
                }
            }
            j = info.otherAnimations.entrySet().iterator();
            while (j.hasNext()) {
                Map.Entry e2 = (Map.Entry) j.next();
                String type = (String) e2.getKey();
                Sandwich sandwich = (Sandwich) e2.getValue();
                if (sandwich.shouldUpdate ||
                        sandwich.animation != null
                            && sandwich.animation.isDirty) {
                    AnimatableValue av = null;
                    AbstractAnimation anim = sandwich.animation;
                    if (anim != null) {
                        av = sandwich.animation.getComposedValue();
                        anim.isDirty = false;
                    }
                    target.updateOtherValue(type, av);
                    sandwich.shouldUpdate = false;
                }
            }
        }
        return waitTime;
    }
    public void toActive(AbstractAnimation anim, float begin) {
        moveToTop(anim);
        anim.isActive = true;
        anim.beginTime = begin;
        anim.isFrozen = false;
        pushDown(anim);
        anim.markDirty();
    }
    protected void pushDown(AbstractAnimation anim) {
        TimedElement e = anim.getTimedElement();
        AbstractAnimation top = null;
        boolean moved = false;
        while (anim.lowerAnimation != null
                && (anim.lowerAnimation.isActive
                    || anim.lowerAnimation.isFrozen)
                && (anim.lowerAnimation.beginTime > anim.beginTime
                    || anim.lowerAnimation.beginTime == anim.beginTime
                        && e.isBefore(anim.lowerAnimation.getTimedElement()))) {
            AbstractAnimation higher = anim.higherAnimation;
            AbstractAnimation lower = anim.lowerAnimation;
            AbstractAnimation lowerLower = lower.lowerAnimation;
            if (higher != null) {
                higher.lowerAnimation = lower;
            }
            if (lowerLower != null) {
                lowerLower.higherAnimation = anim;
            }
            lower.lowerAnimation = anim;
            lower.higherAnimation = higher;
            anim.lowerAnimation = lowerLower;
            anim.higherAnimation = lower;
            if (!moved) {
                top = lower;
                moved = true;
            }
        }
        if (moved) {
            AnimationInfo animInfo = getAnimationInfo(anim);
            Sandwich sandwich = getSandwich(animInfo.target, animInfo.type,
                                            animInfo.attributeNamespaceURI,
                                            animInfo.attributeLocalName);
            if (sandwich.animation == anim) {
                sandwich.animation = top;
            }
            if (anim.lowerAnimation == null) {
                sandwich.lowestAnimation = anim;
            }
        }
    }
    public void toInactive(AbstractAnimation anim, boolean isFrozen) {
        anim.isActive = false;
        anim.isFrozen = isFrozen;
        anim.markDirty();
        if (!isFrozen) {
            anim.value = null;
            anim.beginTime = Float.NEGATIVE_INFINITY;
            moveToBottom(anim);
        }
    }
    public void removeFill(AbstractAnimation anim) {
        anim.isActive = false;
        anim.isFrozen = false;
        anim.value = null;
        anim.markDirty();
        moveToBottom(anim);
    }
    protected void moveToTop(AbstractAnimation anim) {
        AnimationInfo animInfo = getAnimationInfo(anim);
        Sandwich sandwich = getSandwich(animInfo.target, animInfo.type,
                                        animInfo.attributeNamespaceURI,
                                        animInfo.attributeLocalName);
        sandwich.shouldUpdate = true;
        if (anim.higherAnimation == null) {
            return;
        }
        if (anim.lowerAnimation == null) {
            sandwich.lowestAnimation = anim.higherAnimation;
        } else {
            anim.lowerAnimation.higherAnimation = anim.higherAnimation;
        }
        anim.higherAnimation.lowerAnimation = anim.lowerAnimation;
        if (sandwich.animation != null) {
            sandwich.animation.higherAnimation = anim;
        }
        anim.lowerAnimation = sandwich.animation;
        anim.higherAnimation = null;
        sandwich.animation = anim;
    }
    protected void moveToBottom(AbstractAnimation anim) {
        if (anim.lowerAnimation == null) {
            return;
        }
        AnimationInfo animInfo = getAnimationInfo(anim);
        Sandwich sandwich = getSandwich(animInfo.target, animInfo.type,
                                        animInfo.attributeNamespaceURI,
                                        animInfo.attributeLocalName);
        AbstractAnimation nextLower = anim.lowerAnimation;
        nextLower.markDirty();
        anim.lowerAnimation.higherAnimation = anim.higherAnimation;
        if (anim.higherAnimation != null) {
            anim.higherAnimation.lowerAnimation = anim.lowerAnimation;
        } else {
            sandwich.animation = nextLower;
            sandwich.shouldUpdate = true;
        }
        sandwich.lowestAnimation.lowerAnimation = anim;
        anim.higherAnimation = sandwich.lowestAnimation;
        anim.lowerAnimation = null;
        sandwich.lowestAnimation = anim;
        if (sandwich.animation.isDirty) {
            sandwich.shouldUpdate = true;
        }
    }
    public void addTimegraphListener(TimegraphListener l) {
        timedDocumentRoot.addTimegraphListener(l);
    }
    public void removeTimegraphListener(TimegraphListener l) {
        timedDocumentRoot.removeTimegraphListener(l);
    }
    public void sampledAt(AbstractAnimation anim, float simpleTime,
                          float simpleDur, int repeatIteration) {
        anim.sampledAt(simpleTime, simpleDur, repeatIteration);
    }
    public void sampledLastValue(AbstractAnimation anim, int repeatIteration) {
        anim.sampledLastValue(repeatIteration);
    }
    protected abstract TimedDocumentRoot createDocumentRoot();
    protected class Listener implements AnimationTargetListener {
        public void baseValueChanged(AnimationTarget t, String ns, String ln,
                                     boolean isCSS) {
            short type = isCSS ? ANIM_TYPE_CSS : ANIM_TYPE_XML;
            Sandwich sandwich = getSandwich(t, type, ns, ln);
            sandwich.shouldUpdate = true;
            AbstractAnimation anim = sandwich.animation;
            while (anim.lowerAnimation != null) {
                anim = anim.lowerAnimation;
            }
            anim.markDirty();
        }
    }
    protected static class TargetInfo {
        public DoublyIndexedTable xmlAnimations = new DoublyIndexedTable();
        public HashMap cssAnimations = new HashMap();
        public HashMap otherAnimations = new HashMap();
    }
    protected static class Sandwich {
        public AbstractAnimation animation;
        public AbstractAnimation lowestAnimation;
        public boolean shouldUpdate;
        public boolean listenerRegistered;
    }
    protected static class AnimationInfo {
        public AnimationTarget target;
        public short type;
        public String attributeNamespaceURI;
        public String attributeLocalName;
    }
}
