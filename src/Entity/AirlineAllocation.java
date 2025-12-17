package Entity;

import java.util.Date;

public class AirlineAllocation {

    private Long recId;
    private String airlineCode;
    private String airlineName;
    private Integer sortPosition;
    private Boolean enableStatus;

    // Optional fields for telegram (if needed)
    private Integer destination;
    private Integer numAltDest;
    private Integer alt1;
    private Integer alt2;
    private Integer alt3;

    // Metadata fields (timestamps)
    private Date created;
    private Date lastEditTime;
    private Date messageTimeStamp;
    private Date operationTime;
    private String operation;
    private Date originDate;
    private Boolean deleted;

    // ============================
    // Getters and Setters
    // ============================

    public Long getRecId() { return recId; }
    public void setRecId(Long recId) { this.recId = recId; }

    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public Integer getSortPosition() { return sortPosition; }
    public void setSortPosition(Integer sortPosition) { this.sortPosition = sortPosition; }

    public Boolean getEnableStatus() { return enableStatus; }
    public void setEnableStatus(Boolean enableStatus) { this.enableStatus = enableStatus; }

    public Integer getDestination() { return destination; }
    public void setDestination(Integer destination) { this.destination = destination; }

    public Integer getNumAltDest() { return numAltDest; }
    public void setNumAltDest(Integer numAltDest) { this.numAltDest = numAltDest; }

    public Integer getAlt1() { return alt1; }
    public void setAlt1(Integer alt1) { this.alt1 = alt1; }

    public Integer getAlt2() { return alt2; }
    public void setAlt2(Integer alt2) { this.alt2 = alt2; }

    public Integer getAlt3() { return alt3; }
    public void setAlt3(Integer alt3) { this.alt3 = alt3; }

    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    public Date getLastEditTime() { return lastEditTime; }
    public void setLastEditTime(Date lastEditTime) { this.lastEditTime = lastEditTime; }

    public Date getMessageTimeStamp() { return messageTimeStamp; }
    public void setMessageTimeStamp(Date messageTimeStamp) { this.messageTimeStamp = messageTimeStamp; }

    public Date getOperationTime() { return operationTime; }
    public void setOperationTime(Date operationTime) { this.operationTime = operationTime; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public Date getOriginDate() { return originDate; }
    public void setOriginDate(Date originDate) { this.originDate = originDate; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
