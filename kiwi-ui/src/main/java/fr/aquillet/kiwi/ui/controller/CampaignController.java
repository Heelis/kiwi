package fr.aquillet.kiwi.ui.controller;

import de.saxsys.mvvmfx.utils.notifications.NotificationCenter;
import fr.aquillet.kiwi.command.Commands;
import fr.aquillet.kiwi.command.campaign.*;
import fr.aquillet.kiwi.event.Events;
import fr.aquillet.kiwi.event.campaign.*;
import fr.aquillet.kiwi.model.Campaign;
import fr.aquillet.kiwi.toolkit.dispatch.Dispatch;
import fr.aquillet.kiwi.toolkit.dispatch.DispatchUtils;
import fr.aquillet.kiwi.ui.service.application.IApplicationService;
import fr.aquillet.kiwi.ui.service.campaign.ICampaignService;
import fr.aquillet.kiwi.ui.service.persistence.ICampaignPersistenceService;
import fr.aquillet.kiwi.ui.service.scenario.IScenarioService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class CampaignController {

    private NotificationCenter notificationCenter;
    private ICampaignService campaignService;
    private IApplicationService applicationService;
    private ICampaignPersistenceService persistenceService;
    private IScenarioService scenarioService;

    @Inject
    private void setDependencies(final NotificationCenter notificationCenter, //
                                 final ICampaignService campaignService, //
                                 final IApplicationService applicationService, //
                                 final ICampaignPersistenceService persistenceService, //
                                 final IScenarioService scenarioService) {
        this.notificationCenter = notificationCenter;
        this.campaignService = campaignService;
        this.applicationService = applicationService;
        this.persistenceService = persistenceService;
        this.scenarioService = scenarioService;
        notificationCenter.subscribe(Commands.CAMPAIGN, (key, payload) -> DispatchUtils.dispatch(payload[0], this));
    }

    @Dispatch(scheduler = Dispatch.DispatchScheduler.SCHEDULER_CONTROLLER)
    public void handle(CreateCampaignCommand command) {
        Campaign campaign = campaignService.createCampaign(command.getTitle(), command.getLabelId());
        log.info("Creating a new campaign (id: {}, title: {})", campaign.getId(), campaign.getTitle());
        campaignService.getCampaigns().add(campaign);
        notificationCenter.publish(Events.CAMPAIGN, new CampaignCreatedEvent(campaign));
    }

    @Dispatch(scheduler = Dispatch.DispatchScheduler.SCHEDULER_CONTROLLER)
    public void handle(ReloadCampaignsCommand command) {
        log.info("Reloading campaigns");
        campaignService.getCampaigns().clear();
        campaignService.getCampaigns().addAll(persistenceService.getApplicationCampaigns(applicationService.getCurrentApplication()));
        notificationCenter.publish(Events.CAMPAIGN, new CampaignsReloadedEvent());
    }

    @Dispatch(scheduler = Dispatch.DispatchScheduler.SCHEDULER_CONTROLLER)
    public void handle(AddScenarioToCampaignCommand command) {
        campaignService.getCampaignById(command.getCampaignId())
                .ifPresent(campaign -> {
                    log.info("Adding scenario {} to campaign {}", command.getScenarioId(), command.getCampaignId());
                    campaign.getScenarioIds().add(command.getScenarioId());
                    notificationCenter.publish(Events.CAMPAIGN, new ScenarioAddedToCampaignEvent(command.getCampaignId(), command.getScenarioId()));
                });
    }

    @Dispatch(scheduler = Dispatch.DispatchScheduler.SCHEDULER_CONTROLLER)
    public void handle(RemoveScenarioFromCampaignCommand command) {
        campaignService.getCampaignById(command.getCampaignId())
                .ifPresent(campaign -> {
                    log.info("Removing scenario {} from campaign {}", command.getScenarioId(), command.getCampaignId());
                    campaign.getScenarioIds().remove(command.getScenarioId());
                    notificationCenter.publish(Events.CAMPAIGN, new ScenarioRemovedFromCampaignEvent(command.getCampaignId(), command.getScenarioId()));
                });
    }

    @Dispatch(scheduler = Dispatch.DispatchScheduler.SCHEDULER_CONTROLLER)
    public void handle(ReorderCampaignScenariosCommand command) {
        campaignService.getCampaignById(command.getCampaignId())
                .ifPresent(campaign -> {
                    log.info("Reordering scenarios of campaign {}", command.getCampaignId());
                    campaign.getScenarioIds().clear();
                    campaign.getScenarioIds().addAll(command.getScenarioIds());
                    notificationCenter.publish(Events.CAMPAIGN, new CampaignScenariosReorderedEvent(command.getCampaignId(), command.getScenarioIds()));
                });
    }
}
