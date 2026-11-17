package votenow;

import votenow.Models.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VotingDAO {

    public static User authenticate(String username, String password, String role) throws SQLException {
        String sql = "SELECT user_id, username, full_name, role, area, gender FROM users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("area"),
                            rs.getString("gender")
                    );
                }
            }
        }
        return null;
    }

    public static boolean registerVoter(String fullName, String username, String password, String area, String gender) throws SQLException {
        String checkSql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, username);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }

            String insertSql = "INSERT INTO users (username, password, full_name, role, area, gender) VALUES (?, ?, ?, 'VOTER', ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setString(1, username);
                insertPs.setString(2, password);
                insertPs.setString(3, fullName);
                insertPs.setString(4, area);
                insertPs.setString(5, gender);
                return insertPs.executeUpdate() > 0;
            }
        }
    }

    public static List<VoterSummary> getAllVoters(String searchQuery, String zoneFilter) throws SQLException {
        List<VoterSummary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT u.user_id, u.full_name, u.username, u.area, u.gender, COUNT(v.vote_id) AS votes_count " +
                        "FROM users u LEFT JOIN votes v ON u.user_id = v.voter_id " +
                        "WHERE u.role = 'VOTER' "
        );

        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();
        boolean hasZone = zoneFilter != null && !"All Zones".equalsIgnoreCase(zoneFilter);

        if (hasSearch) {
            sql.append("AND (u.full_name LIKE ? OR u.username LIKE ?) ");
        }
        if (hasZone) {
            sql.append("AND u.area = ? ");
        }
        sql.append("GROUP BY u.user_id, u.full_name, u.username, u.area, u.gender ORDER BY u.user_id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasSearch) {
                String pattern = "%" + searchQuery.trim() + "%";
                ps.setString(idx++, pattern);
                ps.setString(idx++, pattern);
            }
            if (hasZone) {
                ps.setString(idx++, zoneFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new VoterSummary(
                            rs.getInt("user_id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("area"),
                            rs.getString("gender"),
                            rs.getInt("votes_count")
                    ));
                }
            }
        }
        return list;
    }

    public static void deleteVoter(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ? AND role = 'VOTER'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public static void syncElectionStatuses() throws SQLException {
        String activateSql = "UPDATE elections SET status = 'IN_PROGRESS' WHERE status = 'UPCOMING' AND start_time <= NOW() AND end_time > NOW()";
        String completeSql = "UPDATE elections SET status = 'COMPLETED' WHERE status IN ('UPCOMING', 'IN_PROGRESS') AND end_time <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(activateSql);
            st.executeUpdate(completeSql);
        }
    }

    public static List<Election> getElectionsByStatus(String status) throws SQLException {
        syncElectionStatuses();
        List<Election> list = new ArrayList<>();
        String sql = "SELECT election_id, title, description, target_area, status, start_time, end_time FROM elections WHERE status = ? ORDER BY start_time ASC, election_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapElection(rs));
                }
            }
        }
        return list;
    }

    public static List<Election> getElectionsForVoter(String status, String voterArea) throws SQLException {
        syncElectionStatuses();
        List<Election> list = new ArrayList<>();
        String sql = "SELECT election_id, title, description, target_area, status, start_time, end_time " +
                "FROM elections WHERE status = ? AND (target_area = 'ALL' OR target_area = ?) " +
                "ORDER BY start_time ASC, election_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, voterArea);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapElection(rs));
                }
            }
        }
        return list;
    }

    private static Election mapElection(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        return new Election(
                rs.getInt("election_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("target_area"),
                rs.getString("status"),
                startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now(),
                endTs != null ? endTs.toLocalDateTime() : LocalDateTime.now()
        );
    }

    public static void createElectionWithCandidates(String title, String description, String targetArea, int startDelayMinutes, int durationMinutes, List<CandidateInput> candidates) throws SQLException {
        String electionSql = "INSERT INTO elections (title, description, target_area, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
        String candSql = "INSERT INTO candidates (election_id, candidate_name, party_name, manifesto) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int electionId;
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = now.plusMinutes(startDelayMinutes);
                LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
                String status = startDelayMinutes > 0 ? "UPCOMING" : "IN_PROGRESS";

                try (PreparedStatement ps = conn.prepareStatement(electionSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, description);
                    ps.setString(3, targetArea);
                    ps.setString(4, status);
                    ps.setTimestamp(5, Timestamp.valueOf(startTime));
                    ps.setTimestamp(6, Timestamp.valueOf(endTime));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to retrieve new election ID.");
                        }
                        electionId = keys.getInt(1);
                    }
                }

                insertCandidatesBatch(conn, candSql, electionId, candidates);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void updateUpcomingElection(int electionId, String title, String description, String targetArea, int startDelayMinutes, int durationMinutes, List<CandidateInput> candidates) throws SQLException {
        String updateSql = "UPDATE elections SET title = ?, description = ?, target_area = ?, status = ?, start_time = ?, end_time = ? WHERE election_id = ?";
        String deleteCandsSql = "DELETE FROM candidates WHERE election_id = ?";
        String candSql = "INSERT INTO candidates (election_id, candidate_name, party_name, manifesto) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = now.plusMinutes(startDelayMinutes);
                LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
                String status = startDelayMinutes > 0 ? "UPCOMING" : "IN_PROGRESS";

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, title);
                    ps.setString(2, description);
                    ps.setString(3, targetArea);
                    ps.setString(4, status);
                    ps.setTimestamp(5, Timestamp.valueOf(startTime));
                    ps.setTimestamp(6, Timestamp.valueOf(endTime));
                    ps.setInt(7, electionId);
                    ps.executeUpdate();
                }

                try (PreparedStatement delPs = conn.prepareStatement(deleteCandsSql)) {
                    delPs.setInt(1, electionId);
                    delPs.executeUpdate();
                }

                insertCandidatesBatch(conn, candSql, electionId, candidates);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static void insertCandidatesBatch(Connection conn, String candSql, int electionId, List<CandidateInput> candidates) throws SQLException {
        try (PreparedStatement cps = conn.prepareStatement(candSql)) {
            for (CandidateInput c : candidates) {
                cps.setInt(1, electionId);
                cps.setString(2, c.candidateName);
                cps.setString(3, c.partyName);
                cps.setString(4, c.manifesto == null || c.manifesto.isBlank() ? "No manifesto provided." : c.manifesto);
                cps.addBatch();
            }
            cps.setInt(1, electionId);
            cps.setString(2, "NOTA");
            cps.setString(3, "None of the Above");
            cps.setString(4, "Official option to formally decline all listed candidates while recording your participation.");
            cps.addBatch();
            cps.executeBatch();
        }
    }

    public static void deleteElection(int electionId) throws SQLException {
        String sql = "DELETE FROM elections WHERE election_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.executeUpdate();
        }
    }

    public static void completeElection(int electionId) throws SQLException {
        String sql = "UPDATE elections SET status = 'COMPLETED', end_time = NOW() WHERE election_id = ? AND status = 'IN_PROGRESS'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.executeUpdate();
        }
    }

    public static List<Candidate> getCandidatesForElection(int electionId) throws SQLException {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT candidate_id, election_id, candidate_name, party_name, manifesto FROM candidates WHERE election_id = ? ORDER BY candidate_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Candidate(
                            rs.getInt("candidate_id"),
                            rs.getInt("election_id"),
                            rs.getString("candidate_name"),
                            rs.getString("party_name"),
                            rs.getString("manifesto")
                    ));
                }
            }
        }
        return list;
    }

    public static boolean hasVoterVoted(int electionId, int voterId) throws SQLException {
        String sql = "SELECT 1 FROM votes WHERE election_id = ? AND voter_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, voterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static String castVote(int electionId, int voterId, int candidateId) throws SQLException {
        if (hasVoterVoted(electionId, voterId)) {
            return null;
        }
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String receiptCode = "VN-" + raw.substring(0, 4) + "-" + raw.substring(4, 8);

        String sql = "INSERT INTO votes (election_id, voter_id, candidate_id, receipt_code) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, voterId);
            ps.setInt(3, candidateId);
            ps.setString(4, receiptCode);
            return ps.executeUpdate() > 0 ? receiptCode : null;
        }
    }

    public static List<VoteReceipt> getVoterReceipts(int voterId) throws SQLException {
        List<VoteReceipt> list = new ArrayList<>();
        String sql = "SELECT e.title, v.receipt_code, v.voted_at " +
                "FROM votes v JOIN elections e ON v.election_id = e.election_id " +
                "WHERE v.voter_id = ? ORDER BY v.voted_at DESC";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("voted_at");
                    String formattedTime = ts != null ? ts.toLocalDateTime().format(fmt) : "Recorded";
                    list.add(new VoteReceipt(
                            rs.getString("title"),
                            rs.getString("receipt_code"),
                            formattedTime
                    ));
                }
            }
        }
        return list;
    }

    public static int getTotalVotesForElection(int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE election_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public static int getTotalRegisteredVoters(String targetArea) throws SQLException {
        boolean allZones = targetArea == null || "ALL".equalsIgnoreCase(targetArea);
        String sql = allZones
                ? "SELECT COUNT(*) FROM users WHERE role = 'VOTER'"
                : "SELECT COUNT(*) FROM users WHERE role = 'VOTER' AND area = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!allZones) {
                ps.setString(1, targetArea);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public static List<ZoneTurnout> getZoneTurnout(int electionId, String targetArea) throws SQLException {
        List<ZoneTurnout> list = new ArrayList<>();
        boolean allZones = targetArea == null || "ALL".equalsIgnoreCase(targetArea);
        String sql = allZones
                ? "SELECT u.area, COUNT(DISTINCT u.user_id) AS registered_voters, COUNT(DISTINCT v.voter_id) AS votes_cast " +
                "FROM users u LEFT JOIN votes v ON u.user_id = v.voter_id AND v.election_id = ? " +
                "WHERE u.role = 'VOTER' AND u.area IS NOT NULL GROUP BY u.area ORDER BY u.area ASC"
                : "SELECT u.area, COUNT(DISTINCT u.user_id) AS registered_voters, COUNT(DISTINCT v.voter_id) AS votes_cast " +
                "FROM users u LEFT JOIN votes v ON u.user_id = v.voter_id AND v.election_id = ? " +
                "WHERE u.role = 'VOTER' AND u.area = ? GROUP BY u.area ORDER BY u.area ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            if (!allZones) {
                ps.setString(2, targetArea);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ZoneTurnout(
                            rs.getString("area"),
                            rs.getInt("registered_voters"),
                            rs.getInt("votes_cast")
                    ));
                }
            }
        }
        return list;
    }

    public static List<CandidateVoteCount> getPartyBreakdown(int electionId) throws SQLException {
        List<CandidateVoteCount> list = new ArrayList<>();
        String sql = "SELECT c.candidate_name, c.party_name, COUNT(v.vote_id) AS vote_count " +
                "FROM candidates c " +
                "LEFT JOIN votes v ON c.candidate_id = v.candidate_id AND v.election_id = c.election_id " +
                "WHERE c.election_id = ? " +
                "GROUP BY c.candidate_id, c.candidate_name, c.party_name " +
                "ORDER BY vote_count DESC, c.candidate_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CandidateVoteCount(
                            rs.getString("candidate_name"),
                            rs.getString("party_name"),
                            rs.getInt("vote_count")
                    ));
                }
            }
        }
        return list;
    }

    public static List<DemographicPartyCount> getAreaBreakdown(int electionId) throws SQLException {
        List<DemographicPartyCount> list = new ArrayList<>();
        String sql = "SELECT u.area, c.party_name, COUNT(v.vote_id) AS vote_count " +
                "FROM votes v " +
                "JOIN users u ON v.voter_id = u.user_id " +
                "JOIN candidates c ON v.candidate_id = c.candidate_id " +
                "WHERE v.election_id = ? AND u.area IS NOT NULL " +
                "GROUP BY u.area, c.party_name " +
                "ORDER BY u.area ASC, vote_count DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DemographicPartyCount(
                            rs.getString("area"),
                            rs.getString("party_name"),
                            rs.getInt("vote_count")
                    ));
                }
            }
        }
        return list;
    }

    public static List<DemographicPartyCount> getGenderBreakdown(int electionId) throws SQLException {
        List<DemographicPartyCount> list = new ArrayList<>();
        String sql = "SELECT u.gender, c.party_name, COUNT(v.vote_id) AS vote_count " +
                "FROM votes v " +
                "JOIN users u ON v.voter_id = u.user_id " +
                "JOIN candidates c ON v.candidate_id = c.candidate_id " +
                "WHERE v.election_id = ? AND u.gender IS NOT NULL " +
                "GROUP BY u.gender, c.party_name " +
                "ORDER BY u.gender ASC, vote_count DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DemographicPartyCount(
                            rs.getString("gender"),
                            rs.getString("party_name"),
                            rs.getInt("vote_count")
                    ));
                }
            }
        }
        return list;
    }
}