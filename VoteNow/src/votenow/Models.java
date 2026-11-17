package votenow;

import java.time.LocalDateTime;

public class Models {

    public static class User {
        public final int userId;
        public final String username;
        public final String fullName;
        public final String role;
        public final String area;
        public final String gender;

        public User(int userId, String username, String fullName, String role, String area, String gender) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.area = area;
            this.gender = gender;
        }
    }

    public static class VoterSummary {
        public final int userId;
        public final String fullName;
        public final String username;
        public final String area;
        public final String gender;
        public final int electionsVotedCount;

        public VoterSummary(int userId, String fullName, String username, String area, String gender, int electionsVotedCount) {
            this.userId = userId;
            this.fullName = fullName;
            this.username = username;
            this.area = area;
            this.gender = gender;
            this.electionsVotedCount = electionsVotedCount;
        }
    }

    public static class Election {
        public final int electionId;
        public final String title;
        public final String description;
        public final String targetArea;
        public final String status;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;

        public Election(int electionId, String title, String description, String targetArea, String status, LocalDateTime startTime, LocalDateTime endTime) {
            this.electionId = electionId;
            this.title = title;
            this.description = description;
            this.targetArea = targetArea;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class Candidate {
        public final int candidateId;
        public final int electionId;
        public final String candidateName;
        public final String partyName;
        public final String manifesto;

        public Candidate(int candidateId, int electionId, String candidateName, String partyName, String manifesto) {
            this.candidateId = candidateId;
            this.electionId = electionId;
            this.candidateName = candidateName;
            this.partyName = partyName;
            this.manifesto = manifesto;
        }
    }

    public static class CandidateInput {
        public final String candidateName;
        public final String partyName;
        public final String manifesto;

        public CandidateInput(String candidateName, String partyName, String manifesto) {
            this.candidateName = candidateName;
            this.partyName = partyName;
            this.manifesto = manifesto;
        }
    }

    public static class CandidateVoteCount {
        public final String candidateName;
        public final String partyName;
        public final int votes;

        public CandidateVoteCount(String candidateName, String partyName, int votes) {
            this.candidateName = candidateName;
            this.partyName = partyName;
            this.votes = votes;
        }
    }

    public static class DemographicPartyCount {
        public final String categoryValue;
        public final String partyName;
        public final int votes;

        public DemographicPartyCount(String categoryValue, String partyName, int votes) {
            this.categoryValue = categoryValue;
            this.partyName = partyName;
            this.votes = votes;
        }
    }

    public static class ZoneTurnout {
        public final String area;
        public final int registeredVoters;
        public final int votesCast;

        public ZoneTurnout(String area, int registeredVoters, int votesCast) {
            this.area = area;
            this.registeredVoters = registeredVoters;
            this.votesCast = votesCast;
        }
    }

    public static class VoteReceipt {
        public final String electionTitle;
        public final String receiptCode;
        public final String votedAt;

        public VoteReceipt(String electionTitle, String receiptCode, String votedAt) {
            this.electionTitle = electionTitle;
            this.receiptCode = receiptCode;
            this.votedAt = votedAt;
        }
    }
}