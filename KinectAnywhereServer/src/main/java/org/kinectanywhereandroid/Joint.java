package org.kinectanywhereandroid;


public class Joint {
	
    // This contains all of the possible joint types.
    public enum JointType
    {
        HipCenter(0),
        Spine(1),
        ShoulderCenter(2),
        Head(3),
        ShoulderLeft(4),
        ElbowLeft(5),
        WristLeft(6),
        HandLeft(7),
        ShoulderRight(8),
        ElbowRight(9),
        WristRight(10),
        HandRight(11),
        HipLeft(12),
        KneeLeft(13),
        AnkleLeft(14),
        FootLeft(15),
        HipRight(16),
        KneeRight(17),
        AnkleRight(18),
        FootRight(19);

        private final int value;
        JointType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // The tracking state of a specific joint.
    public enum JointTrackingState
    {
        NotTracked(0),
        Inferred(1),
        Tracked(2);

        private final int value;
        JointTrackingState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    JointType type;
    JointTrackingState trackingState;
    float x;
    float y;
    float z;
    
    Joint(float x, float y, float z) {
    	
    	this.x = x;
    	this.y = y;
    	this.z = z;
    }
    
    Joint() { 
    	
    	this(0f, 0f, 0f);
    };
}
